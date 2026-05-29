package com.diwan.gateway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_features")
public class AppFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 10)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "screen_name", length = 100)
    private String screenName;

    @Column(length = 200)
    private String path;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    public AppFeature() {}

    public AppFeature(String name, String displayName, String icon, String color,
                      String screenName, String path, String description, int sortOrder) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
        this.screenName = screenName;
        this.path = path;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public String getScreenName() { return screenName; }
    public String getPath() { return path; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public void setPath(String path) { this.path = path; }
    public void setDescription(String description) { this.description = description; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }
}