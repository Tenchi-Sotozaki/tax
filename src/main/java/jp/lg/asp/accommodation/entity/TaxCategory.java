package jp.lg.asp.accommodation.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_category")
@Getter
@Setter
public class TaxCategory {

	@Id
	@Column(name = "tax_category_code", length = 2)
	private String taxCategoryCode;

	@Column(name = "category_name", nullable = false, length = 100)
	private String categoryName;

	@Column(name = "tax_amount", nullable = false, precision = 10, scale = 0)
	private BigDecimal taxAmount;
}
