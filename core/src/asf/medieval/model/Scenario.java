package asf.medieval.model;

import asf.medieval.model.steer.InfantryController;
import asf.medieval.model.steer.SteerGraph;
import asf.medieval.model.steer.StructureController;
import asf.medieval.shape.Box;
import asf.medieval.terrain.Terrain;
import asf.medieval.terrain.TerrainChunk;
import asf.medieval.utility.UtMath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/**
 * Created by Daniel Strong on 11/11/2015.
 */
public class Scenario {
	private transient Listener listener;

	public final ScenarioRand rand;
	public transient Terrain terrain;
	public final SteerGraph steerGraph = new SteerGraph();

	public IntMap<Player> players = new IntMap<Player>(2);

	public Array<Token> tokens = new Array<Token>(false, 256, Token.class);

	public Scenario(ScenarioRand rand) {
		this.rand = rand;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
		if (this.listener == null)
			return;

		for (Player player : players.values()) {
			listener.onNewPlayer(player);
		}

		for (Token token : tokens) {
			listener.onNewToken(token);
		}

	}

	public void addPlayer(Player player){
		Player existingPlayer = players.get(player.id);

		if(existingPlayer == null)
		{
			Player newPlayer = player.cpy();
			players.put(newPlayer.id, newPlayer);
			if(listener!=null)
				listener.onNewPlayer(newPlayer);
		}
		else
		{
			existingPlayer.set(player);
			if(listener!=null)
				listener.onUpdatePlayer(existingPlayer);
		}
	}

	public void removePlayer(Player player){
		Player removedPlayed = players.remove(player.id);
		if(removedPlayed!=null && listener!=null)
			listener.onRemovePlayer(removedPlayed);
	}

	public Player getPlayer(int id){
		return players.get(id);
	}

	public Token getSoldier(int tokenId)
	{
		for (Token token : tokens) {
			if(token.id == tokenId){
				return token;
			}
		}
		return null;
	}

	private int lastTokenId = 0;

	public Token newSoldier(int owner, Vector2 location, boolean jimmy)
	{
		Token token= new Token();
		++lastTokenId;
		token.id = lastTokenId;
		token.scenario = this;
		token.owner = players.get(owner);
		token.modelId = owner == 1 ? ModelId.Skeleton : ModelId.Knight;
		if(jimmy)
			token.modelId = ModelId.Jimmy;
		token.shape = new Box(1f, 7.5f);
		token.location.set(location);
		token.attack = new AttackController(token);
		token.damage = new DamageController(token);
		token.agent = new InfantryController(token);
		steerGraph.agents.add(token.agent);
		tokens.add(token);

		setNonOverlappingPosition(token, location);
		token.elevation = terrain.getElevation(location.x,location.y);

		if(listener!=null)
			listener.onNewToken(token);
		return token;
	}

	public Token newStructure(int owner, Vector2 location)
	{
		Token token= new Token();
		++lastTokenId;
		token.id = lastTokenId;
		token.scenario = this;
		token.modelId = ModelId.Church;
		float width = 9.5f;
		float height = 10f;
		float depth = 10.2f;
		token.shape = new Box( width, height, depth, width*0.05f, height, -depth*0.75f);
		token.owner = players.get(owner);
		token.location.set(location);
		token.elevation = terrain.getElevation(location.x,location.y);
		token.agent = new StructureController(token);
		steerGraph.agents.add(token.agent);
		tokens.add(token);

		if(listener!=null)
			listener.onNewToken(token);
		return token;
	}

	public void setRandomNonOverlappingPosition (Token token, float minX, float maxX, float minY, float maxY) {
		int maxTries = UtMath.largest(100, tokens.size * tokens.size);
		final float eps = 0.001f;
		SET_NEW_POS:
		while (--maxTries >= 0) {
			token.location.set(rand.range(minX,maxX),rand.range(minY,maxY));
			for (Token t : tokens) {
				if(t!= token){
					Token other =  t;
					if(UtMath.abs(token.location.x-other.location.x) <eps && UtMath.abs(token.location.y-other.location.y) <eps){
						continue SET_NEW_POS;
					}
				}
			}
			return;
		}
		throw new IllegalStateException("Probable infinite loop detected");
	}

	public void setNonOverlappingPosition(Token token, Vector2 location )
	{
		// THis is a workaround for the bug where if two tokens are in the same location, one of them
		// will end up with a location of "NaN" i suppose due to the steering system.
		// ideally id like to figure out whats causing the NaN and let steering just naturally seperate
		// the soldiers...
		token.location.set(location);
		int maxTries = UtMath.largest(100, tokens.size * tokens.size);
		final float eps = 0.001f;
		SET_NEW_POS:
		while (--maxTries >= 0) {
			for (Token t : tokens) {

				if(t!= token){
					Token other =  t;
					//if(token.location.dst(other.location) <= token.radius){
					if(UtMath.abs(token.location.x-other.location.x) <eps && UtMath.abs(token.location.y-other.location.y) <eps){
						token.location.x = location.x + rand.range(-token.shape.radius,token.shape.radius);
						token.location.y = location.y + rand.range(-token.shape.radius,token.shape.radius);
						continue SET_NEW_POS;
					}
				}
			}
			return;
		}

		throw new IllegalStateException("Probable infinite loop detected");
	}


	public void update(float delta)
	{


		for (Token token : tokens) {
			token.update(delta);
		}
	}

	public static interface Listener {
		public void onNewPlayer(Player player);
		public void onUpdatePlayer(Player player);
		public void onRemovePlayer(Player player);
		public void onNewToken(Token token);
	}
}