package asf.medieval.view;

import asf.medieval.model.InfantryAgent;
import asf.medieval.model.ModelId;
import asf.medieval.model.Token;
import asf.medieval.shape.Shape;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Created by Daniel Strong on 11/11/2015.
 */
public class InfantryView implements View, SelectableView, AnimationController.AnimationListener {
	private MedievalWorld world;
	public final Token token;
	public final InfantryAgent agent;
	public final Shape shape;
	private ModelInstance modelInstance;
	private AnimationController animController;
	public final Vector3 translation = new Vector3();
	public final Quaternion rotation = new Quaternion();

	public boolean selected = false;
	private final Decal selectionDecal = new Decal();

	private String[] idle = new String[]{"Idle"};
	private final String[] walk = new String[]{"Walk"};
	private final String[] attack = new String[]{"Attack"};
	private final String[] hit = new String[]{"Hit"};
	private final String[] die = new String[]{"Die"};

	public InfantryView(MedievalWorld world, Token soldierToken) {
		this.world = world;
		this.token = soldierToken;
		agent = (InfantryAgent)token.agent;
		shape = token.shape;

		//world.addGameObject(new DebugShapeView(world).shape(token.location,token.shape));
		String asset = "Models/Characters/Skeleton.g3db";
		if(token.modelId == ModelId.Jimmy)
		{
			asset = "Models/Jimmy/Jimmy_r1.g3db";
			idle = new String[]{"Idle01","Idle02","Idle03"};
			walk[0] = "MoveForward";
			attack[0] = "FlareThrow";
			hit[0] = "FlareRelease";
			die[0] = "TurtleOnDizzyWithFall";
		}


		Model model = world.assetManager.get(asset);

		modelInstance = new ModelInstance(model);
		if (modelInstance.animations.size > 0) {
			animController = new AnimationController(modelInstance);
			animController.animate(idle[MathUtils.random.nextInt(idle.length)], 0, -1, -1, 1, this, 0.2f);

		}
		//rotation.setEulerAngles(180f, 0, 0);




		//shape = new Disc(token.radius);
		//shape = new Sphere(token.radius,0, token.height/2f, 0);

		selectionDecal.setTextureRegion(world.pack.findRegion("Textures/MoveCommandMarker"));
		selectionDecal.setBlending(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		selectionDecal.setDimensions(token.shape.radius *3.5f, token.shape.radius *3.5f);
		selectionDecal.setColor(1, 1, 0, 1);
		selectionDecal.rotateX(-90);

	}

	@Override
	public void update(float delta) {
		translation.set(token.location);


		//System.out.println(token.id + ": " + token.location);



		float speed = agent.getVelocity().len();
		if(speed < .75f)
		{
			if (!animController.current.animation.id.startsWith("Idle"))
				animController.animate(idle[MathUtils.random.nextInt(idle.length)], 0, -1, -1, 1, this, 0.1f);
		}
		else
		{
			if (!animController.current.animation.id.startsWith(walk[0])){
				if(token.modelId == ModelId.Skeleton)
					animController.animate(walk[0], 0, -1, -1, 1, this, 0.2f);
				else{
					animController.animate(walk[0], 0, -1, 1, 1, this, 0.2f);
				}
			}else{
				animController.current.speed = speed  /agent.getMaxSpeed() * 0.65f;
			}


			Vector3 dir = agent.getVelocity().cpy();
			dir.x *= -1f; // TODO: why do i need to flip the x direction !?!?!
			dir.y  =0;
			dir.nor();
			rotation.setFromCross(dir, Vector3.Z);
		}


		if (animController != null)
			animController.update(delta);
	}


	private static Vector3 vec1 = new Vector3();
	@Override
	public void render(float delta) {
		modelInstance.transform.set(
			translation.x, translation.y, translation.z,
			rotation.x, rotation.y, rotation.z, rotation.w,
			1, 1, 1
		);
		world.shadowBatch.render(modelInstance);
		world.modelBatch.render(modelInstance, world.environment);

		if(selected)
		{
			selectionDecal.setPosition(translation);
			world.scenario.heightField.getWeightedNormalAt(translation, vec1);
			selectionDecal.setRotation(vec1, Vector3.Y);
			vec1.scl(0.1f);
			selectionDecal.translate(vec1);
			world.decalBatch.add(selectionDecal);
		}
	}

	/**
	 * @return -1 on no intersection,
	 * or when there is an intersection: the squared distance between the center of this
	 * object and the point on the ray closest to this object when there is intersection.
	 */
	public float intersects(Ray ray) {
		return shape.intersects(modelInstance.transform, ray);
	}

	@Override
	public Token getToken() {
		return token;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public Vector3 getTranslation() {
		return translation;
	}

	@Override
	public void onEnd(AnimationController.AnimationDesc animation) {
		//animController.animate("SomethingElse", 0, -1, 1, 1, this, 0.2f);
	}

	@Override
	public void onLoop(AnimationController.AnimationDesc animation) {
		if (animation.animation.id.startsWith("Idle"))
			animController.animate(idle[MathUtils.random.nextInt(idle.length)], 0, -1, -1, 1, this, 0.2f);
	}
}
