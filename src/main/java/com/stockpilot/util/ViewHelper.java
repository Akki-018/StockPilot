package com.stockpilot.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Reusable JavaFX UI helper methods supporting the Industrial Minimalism design system.
 */
public final class ViewHelper {

    // SVG icon path data (standard 24x24 vector paths)
    public static final String ICON_DASHBOARD =
            "M3 3h7v7H3zm9 0h7v7h-7zm-9 9h7v7H3zm9 0h7v7h-7z";
    public static final String ICON_PRODUCTS =
            "M12 2L2 7.5v9L12 22l10-5.5v-9L12 2zm0 2.3l7.6 4.2-7.6 4.2L4.4 8.5 12 4.3zM4 10.3l7 3.9v6.8l-7-3.9v-6.8zm16 6.8l-7 3.9v-6.8l7-3.9v6.8z";
    public static final String ICON_TRANSACTIONS =
            "M5 7h11l-3-3 1.4-1.4L19.8 8l-5.4 5.4L13 12l3-3H5V7zm14 10H8l3 3-1.4 1.4L4.2 16l5.4-5.4L11 12l-3 3h11v2z";
    public static final String ICON_SUPPLIERS =
            "M2 20h20v2H2v-2zm2-2V9l6 4V9l6 4V3h4v15H4zm12-11h2v2h-2V7zm0 4h2v2h-2v-2zm0 4h2v2h-2v-2z";
    public static final String ICON_RIPPLE =
            "M13 2L3 14h8l-2 8 10-12h-8l2-8z";
    public static final String ICON_SUN =
            "M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 00-1.41-1.41l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 00-1.41-1.41l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z";
    public static final String ICON_MOON =
            "M12.3 2a10 10 0 00-1.9 19.8 10 10 0 0010.5-12.7A8.1 8.1 0 0112.3 2z";
    public static final String ICON_SEARCH =
            "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";

    private ViewHelper() {
        // Utility class; do not instantiate.
    }

    /**
     * Builds a crisp SVGPath node wrapped inside a fixed-dimension container for consistent alignment.
     */
    public static Node createIcon(String pathData, String styleClass, double scale) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().add(styleClass);
        if (scale != 1.0) {
            icon.setScaleX(scale);
            icon.setScaleY(scale);
        }

        StackPane container = new StackPane(icon);
        container.setPrefSize(18, 18);
        container.setMinSize(18, 18);
        container.setMaxSize(18, 18);
        container.setAlignment(Pos.CENTER);
        return container;
    }

    /**
     * Constructs a single metric segment for the industrial metrics strip.
     * Uses editorial typography hierarchy, clean spacing, and subtle status modifiers.
     */
    public static VBox createStatSegment(String label, String value, String subtext, String valueModifierClass) {
        VBox segment = new VBox(6);
        segment.getStyleClass().add("metric-segment");
        segment.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(segment, Priority.ALWAYS);

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("metric-label");

        topRow.getChildren().add(labelNode);

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("metric-value");
        if (valueModifierClass != null && !valueModifierClass.isEmpty()) {
            valueNode.getStyleClass().add(valueModifierClass);
        }

        Label subtextNode = new Label(subtext);
        subtextNode.getStyleClass().add("metric-subtext");

        segment.getChildren().addAll(topRow, valueNode, subtextNode);
        return segment;
    }

    /**
     * Builds a placeholder view for upcoming roadmap modules (Products, Transactions, Suppliers, Ripple).
     */
    public static Node createPlaceholderView(String title, String badgeText, String description) {
        VBox container = new VBox(20);
        container.getStyleClass().add("content-container");
        container.setPadding(new Insets(28));

        VBox card = new VBox(14);
        card.getStyleClass().add("placeholder-card");
        card.setMaxWidth(720);

        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);

        Label badgeLabel = new Label(badgeText);
        badgeLabel.getStyleClass().add("module-badge");

        Label roadmapLabel = new Label("// SYSTEM ROADMAP STAGE");
        roadmapLabel.getStyleClass().add("meta-tag");

        metaHeader.getChildren().addAll(badgeLabel, roadmapLabel);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("placeholder-description");
        descriptionLabel.setWrapText(true);

        HBox specFooter = new HBox(12);
        specFooter.getStyleClass().add("placeholder-footer");
        specFooter.setAlignment(Pos.CENTER_LEFT);

        Label spec1 = new Label("Architecture: Model-Dao-Service");
        spec1.getStyleClass().add("footer-spec");
        Label divider = new Label("|");
        divider.getStyleClass().add("footer-spec-divider");
        Label spec2 = new Label("Persistence: SQLite Integration Pending");
        spec2.getStyleClass().add("footer-spec");

        specFooter.getChildren().addAll(spec1, divider, spec2);

        card.getChildren().addAll(metaHeader, titleLabel, descriptionLabel, specFooter);
        container.getChildren().add(card);
        return container;
    }
}
