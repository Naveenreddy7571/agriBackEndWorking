package com.ecom.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 500)
	private String title;

	@Column(length = 5000)
	private String description;

	private String category;

	private Double price;

	private int stock;



	@Override
	public String toString() {
		return "Product{" +
				"id=" + id +
				", title='" + title + '\'' +
				", description='" + description + '\'' +
				", category='" + category + '\'' +
				", price=" + price +
				", stock=" + stock +
				", image='" + image + '\'' +
				", discount=" + discount +
				", discountPrice=" + discountPrice +
				", isActive=" + isActive +
				'}';
	}

	private String image;

	private int discount;

	private Double discountPrice;

	private Boolean isActive;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserDtls addedBy;
}
