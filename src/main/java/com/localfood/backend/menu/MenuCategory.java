package com.localfood.backend.menu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="menu_categories") @Getter @Setter
public class MenuCategory {
    @Id @Column(length=100) private String name;
    @Column(nullable=false, unique=true, length=100) private String normalizedName;
}
