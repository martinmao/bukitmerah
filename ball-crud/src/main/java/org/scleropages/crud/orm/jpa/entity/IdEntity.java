package org.scleropages.crud.orm.jpa.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * 
 * @author <a href="mailto:dev.martinmao@gmail.com">Martin Mao</a>
 *
 */
@MappedSuperclass
public abstract class IdEntity {

	public static final int SEQ_DEFAULT_ALLOCATION_SIZE = 20;
	public static final int SEQ_DEFAULT_INITIAL_VALUE = 1000;

	protected Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	//@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
