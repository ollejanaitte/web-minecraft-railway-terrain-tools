package net.minecraft.railsys.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.railsys.geometry.RailGeometry;

/**
 * RailNetwork — a lightweight registry of RailPieces plus endpoint connections.
 * Phase 1.2 scope: add/remove/get pieces, connect/disconnect endpoints,
 * adjacency lookup and network-wide validation. NO A* routing, NO switch
 * branch logic, NO persistence (those are later phases). An endpoint may hold
 * multiple connections so a future switch piece can expose several possible
 * outgoing connections without a schema change.
 */
public final class RailNetwork {

	private final Map<Integer, RailPiece> pieces = new LinkedHashMap<>();
	private final List<RailConnection> connections = new ArrayList<>();

	public RailPiece addPiece(RailGeometry geometry) {
		if (geometry == null) {
			throw new IllegalArgumentException("addPiece geometry must not be null");
		}
		RailPiece piece = new RailPiece(geometry);
		if (this.pieces.containsKey(Integer.valueOf(piece.pieceId()))) {
			throw new IllegalArgumentException("duplicate pieceId=" + piece.pieceId());
		}
		this.pieces.put(Integer.valueOf(piece.pieceId()), piece);
		return piece;
	}

	public boolean addPiece(RailPiece piece) {
		if (piece == null) {
			return false;
		}
		if (this.pieces.containsKey(Integer.valueOf(piece.pieceId()))) {
			return false;
		}
		this.pieces.put(Integer.valueOf(piece.pieceId()), piece);
		return true;
	}

	public boolean removePiece(int pieceId) {
		RailPiece piece = this.pieces.remove(Integer.valueOf(pieceId));
		if (piece == null) {
			return false;
		}
		List<RailConnection> toRemove = new ArrayList<>();
		for (RailConnection c : this.connections) {
			if (c.a().pieceId() == pieceId || c.b().pieceId() == pieceId) {
				toRemove.add(c);
			}
		}
		for (RailConnection c : toRemove) {
			c.detach();
			this.connections.remove(c);
		}
		return true;
	}

	public RailPiece getPiece(int pieceId) {
		return this.pieces.get(Integer.valueOf(pieceId));
	}

	public Collection<RailPiece> pieces() {
		return Collections.unmodifiableCollection(this.pieces.values());
	}

	public int pieceCount() {
		return this.pieces.size();
	}

	/**
	 * Connect two endpoints. Returns a validation result: invalid (never
	 * creating a connection) for null endpoints, unknown pieces, self
	 * connection, duplicate connection, position/tangent misalignment or
	 * endpoint-state conflicts.
	 */
	public RailValidationResult connect(RailEndpoint a, RailEndpoint b) {
		if (a == null || b == null) {
			return RailValidationResult.invalid("null endpoint");
		}
		if (this.pieces.get(Integer.valueOf(a.pieceId())) == null
				|| this.pieces.get(Integer.valueOf(b.pieceId())) == null) {
			return RailValidationResult.invalid("endpoint belongs to an unknown piece (not in this network)");
		}
		if (a.equals(b)) {
			return RailValidationResult.invalid("self connection");
		}
		for (RailConnection c : this.connections) {
			if ((c.a().equals(a) && c.b().equals(b)) || (c.a().equals(b) && c.b().equals(a))) {
				return RailValidationResult.invalid("duplicate connection " + a.pieceId() + "<->" + b.pieceId());
			}
		}
		RailValidationResult v = RailConnection.validate(a, b);
		if (!v.valid) {
			return v;
		}
		RailConnection c = RailConnection.create(a, b);
		this.connections.add(c);
		return RailValidationResult.ok();
	}

	public boolean disconnect(RailConnection connection) {
		if (connection == null || !this.connections.contains(connection)) {
			return false;
		}
		connection.detach();
		this.connections.remove(connection);
		return true;
	}

	/** Adjacency lookup: all connections attached to an endpoint (unmodifiable). */
	public List<RailConnection> connectionsOf(RailEndpoint e) {
		if (e == null) {
			return Collections.emptyList();
		}
		return e.connections();
	}

	public Collection<RailConnection> connections() {
		return Collections.unmodifiableCollection(this.connections);
	}

	public int connectionCount() {
		return this.connections.size();
	}

	/** Network-wide validation: every piece valid and every connection valid. */
	public RailValidationResult validate() {
		for (RailPiece p : this.pieces.values()) {
			RailValidationResult v = p.validate();
			if (!v.valid) {
				return RailValidationResult.invalid("piece " + p.pieceId() + ": " + v.reason);
			}
		}
		for (RailConnection c : this.connections) {
			if (!c.isValid()) {
				return RailValidationResult.invalid("connection " + c.a().pieceId() + "<->" + c.b().pieceId()
						+ ": " + c.validation().reason);
			}
		}
		return RailValidationResult.ok();
	}
}
