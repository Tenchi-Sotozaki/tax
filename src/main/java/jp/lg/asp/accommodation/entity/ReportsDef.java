package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_reports_def")
@IdClass(ReportsDefId.class)
@Getter
@Setter
public class ReportsDef extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd", length = 5)
	private String jichitaiCd;

	@Id
	@Column(name = "id", length = 10)
	private String id;

	@Column(name = "kbn", nullable = false, length = 1)
	private String kbn;

	@Column(name = "def_text", length = 1000)
	private String defText;

	@Column(name = "def_data")
	private byte[] defData;

}