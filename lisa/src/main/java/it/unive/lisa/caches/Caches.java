package it.unive.lisa.caches;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.collections.ExternalSetCache;

/**
 * A holder of {@link ExternalSetCache}s, to ensure that all
 * {@link ExternalSet}s built to hold a given element type will share the same
 * <i>unique</i> cache.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Caches {

	private static ExternalSetCache<Type> types;
	
	public static void init() {
		types = new ExternalSetCache<>();
	}

	/**
	 * Yields the globally defined cache for {@link ExternalSet}s containing
	 * {@link Type}s.
	 * 
	 * @return the types cache, or {@code null} if {@link #init()} has never
	 *             been called
	 */
	public static ExternalSetCache<Type> types() {
		return types;
	}
}
