package restaurant.server.session;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public abstract class GenericDaoBean<T, ID extends Serializable> implements GenericDaoLocal<T, ID> {
	
	private Class<T> entityType;

	@PersistenceContext(unitName = "restaurant")
	protected EntityManager em;

	@SuppressWarnings("unchecked")
	public GenericDaoBean() {
		entityType = (Class<T>) ((ParameterizedType) getClass()
				.getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public Class<T> getEntityType() {
		return entityType;
	}

	public T findById(ID id) {
		T entity;
		entity = em.find(entityType, id);
		return entity;
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		Query q = em.createQuery("SELECT x FROM " + entityType.getSimpleName()
				+ " x");
		List<T> result = q.getResultList();
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<T> findBy(String query) {
		Query q = em.createQuery(query);
		List<T> result = q.getResultList();
		return result;
	}

	public T persist(T entity) {
		em.persist(entity);
		return entity;
	}

	public T merge(T entity) {
		entity = em.merge(entity);
		return entity;
	}

	public boolean remove(T entity) {
		try {
			entity = em.merge(entity);
			em.remove(entity);
			return true;
		}catch (Exception ex){
			return false;
		}
	}

	public void flush() {
		em.flush();
	}

	public void clear() {
		em.clear();
	}

}
