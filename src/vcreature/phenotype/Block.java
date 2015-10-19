package vcreature.phenotype;



import com.jme3.bullet.PhysicsSpace;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.scene.Node;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import java.util.ArrayList;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;

/**
 * The Class Block.
 */
public class Block
{
  /**
   * @deprecated 
   * should use PhysicsConstants.BLOCK_DENSITY 
   */
  public static final float BLOCK_DENSITY = 4f; //Killograms per cubic meter.
  /**
   *
   */
  public static Material MATERIAL_RED;
  /**
   *
   */
  public static Material MATERIAL_BLUE;
  /**
   *
   */
  public static Material MATERIAL_GREEN;
  /**
   *
   */
  public static Material MATERIAL_BROWN;
  /**
   *
   */
  public static Material MATERIAL_GRAY;
  
  private float sizeX, sizeY, sizeZ; //meters
  private final Vector3f startCenter; //meters
  private int id; //Assigned when added to Creature. 0=root and +1 for each block added in order the blocks are added. This is used by DNA and logic curits
  
  private Block parent;
  private HingeJoint jointToParent;
  private ArrayList<Neuron> neuronTable = new ArrayList<>();
  private ArrayList<Block> childList    = new ArrayList<>();
  private Geometry geometry;
  private RigidBodyControl physicsControl;
  
  //Temporary vectors used on each frame. They here to avoid instanciating new vectors on each frame
  private Vector3f tmpVec3 = new Vector3f(); //
  private Quaternion tmpQuat = new Quaternion();
  
  
  //Creates a box that has a center of 0,0,0 and extends in the out from 
    //the center by the given amount in each direction. 
    // So, for example, a box with extent of 0.5 would be the unit cube.
  /**
   *
   * @param physicsSpace
   * @param rootNode
   * @param id
   * @param center
   * @param size
   * @param rotation
   */
  public Block(PhysicsSpace physicsSpace, Node rootNode, int id, Vector3f center, Vector3f size, Quaternion rotation) 
  { 
    if (size.x < 0.5f || size.y < 0.5f || size.z < 0.5f) 
    { throw new IllegalArgumentException("No dimension may be less than 0.5 from block's center: ("+vectorToStr(size));
    }
    
    if (max(size) > 10*min(size))
    {  throw new IllegalArgumentException("Largest dimension must be no more than 10x the smallest: ("+vectorToStr(size)+")");
    }
    
    this.id = id;
    
    startCenter = center;
    
    sizeX = size.x*2;
    sizeY = size.y*2;
    sizeZ = size.z*2;

    
    //Creates a box that has a center of 0,0,0 and extends in the out from 
    //the center by the given amount in each direction. 
    // So, for example, a box with extent of 0.5 would be the unit cube.
    Box box = new Box(size.x, size.y, size.z);
    geometry = new Geometry("Box", box);
    geometry.setMaterial(MATERIAL_GRAY);
    rootNode.attachChild(geometry);
    geometry.setShadowMode(ShadowMode.Cast);
    geometry.rotate(rotation);
    geometry.move(startCenter);
    
    physicsControl = new RigidBodyControl(getMass());
    geometry.addControl(physicsControl);
    
    
    physicsControl.setPhysicsRotation(rotation);
    physicsControl.setPhysicsLocation(startCenter);
    physicsSpace.add(physicsControl);
    physicsControl.setRestitution(PhysicsConstants.BLOCK_BOUNCINESS);
    physicsControl.setFriction(PhysicsConstants.SLIDING_FRICTION);
    physicsControl.setDamping(PhysicsConstants.LINEAR_DAMPINING, 
            PhysicsConstants.ANGULAR_DAMPINING);
  }
  
  
  /**
   *
   */
  public void clear()
  {
    childList.clear();
    neuronTable.clear();
    
    jointToParent = null;
    
    sizeX=0; sizeY=0; sizeZ=0;
    startCenter.x = 0; startCenter.y = 0; startCenter.z = 0;
    id = 0;
    parent = null;
    geometry = null;
    physicsControl = null;
  }
  
  /**
   *
   * @param mat
   */
  public void setMaterial(Material mat)
  {
    geometry.setMaterial(mat);
  }


  /**
   *
   * @param parent
   * @param joint
   */
  public void setJointToParent(Block parent, HingeJoint joint)
  {
     jointToParent = joint;
     parent.childList.add(this);
     this.parent = parent;
  }

  /**
   *
   * @param neuron
   */
  public void addNeuron(Neuron neuron)
  {
     neuronTable.add(neuron);
  }
  
  /**
   *
   * @return
   */
  public Geometry getGeometry() {return geometry;}
  /**
   *
   * @return
   */
  public RigidBodyControl getPhysicsControl() {return physicsControl;}
  /**
   *
   * @return
   */
  public HingeJoint getJoint(){ return jointToParent;}
  /**
   *
   * @return
   */
  public float getJointAngle() { return jointToParent.getHingeAngle(); }
  /**
   *
   * @param output
   * @return
   */
  public Vector3f getCenter(Vector3f output) {return physicsControl.getPhysicsLocation(output); }
  /**
   *
   * @param output
   * @return
   */
  public Vector3f getStartCenter(Vector3f output) 
  { output.x = startCenter.x;
    output.y = startCenter.y;
    output.z = startCenter.z;
    return output; 
  }
  
  
  /**
   *
   * @return
   */
  public float getHeight()
  {
    BoundingBox box = (BoundingBox) geometry.getWorldBound();
    tmpVec3 = box.getMin(tmpVec3);
    return tmpVec3.y;
  }
  
  
  /**
   *
   * @return
   */
  public int getID() {return id;}
  
  
   /**
   * 
   * setID() should only be used by the creature class when one or more blocks are deleated form the creature.
   */
  void setID(int id) {this.id = id;}

  /**
   * 
   * @return the id of the parent or -1 if this is a root block.
   */
  public int getIdOfParent()
  { 
    if (parent == null) return -1;
    return parent.getID();
  }
  

  /**
   *
   * @return Full extent (not half extent) of the block along the x-axis of 
   * its local coordinates (before any rotations).
   */
  public float getSizeX() {return sizeX;}
  

  /**
   * 
   * @return Full extent (not half extent) of the block along the y-axis of 
   * its local coordinates (before any rotations).
   */
  public float getSizeY() {return sizeY;}
  

  /**
   *
   * @return Full extent (not half extent) of the block along the z-axis of 
   * its local coordinates (before any rotations).
   */
  public float getSize() {return sizeZ;}
  
  /**
   * Careful when using this. It gives a pointer to the block's childList.
   * If this is corrupted the results are continued use of the creature class 
   * are undefined.
   * @return
   */
  public ArrayList<Block> getChildList() {return childList;}
  
  
  /**
   * @return
   */
  public ArrayList<Neuron> getNeuronTable() { return neuronTable;}
  
  
  
  
  /**
   *
   * @param assetManager
   */
  public static void initStaticMaterials(AssetManager assetManager)
  {
    MATERIAL_RED   = initStaticMaterial(assetManager, ColorRGBA.Red);
    MATERIAL_BLUE  = initStaticMaterial(assetManager, ColorRGBA.Blue);
    MATERIAL_GREEN = initStaticMaterial(assetManager, ColorRGBA.Green);
    MATERIAL_BROWN = initStaticMaterial(assetManager, ColorRGBA.Brown);
    MATERIAL_GRAY  = initStaticMaterial(assetManager, ColorRGBA.Gray);
  }
  
  private static Material initStaticMaterial(AssetManager assetManager, ColorRGBA color)
  {
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    mat.setColor("Specular", color);
    mat.setFloat("Shininess", 64f);  // [0,128]
    return mat;
  }
  
  
  public String toString()
  {
    String s = "Block["+id+"]: {" + sizeX + ", " + sizeY + ", " + sizeZ + "}\n";
    
    if (parent == null)
    { s += "     ROOT\n\n";
    }
    else
    {
      s += "     Parent: [ID=" + parent.getID() + "]: " + jointToParent + "\n"; 
    }
    return s;
  }
  
  
   //Return the maximium impulse that can be supplyed by a joint on the given parent.
  //The maximium impulse that can be applyed to a joint is proportional to the parent's surface area.
  //The maximium impulse is returned in Newton seconds.
  /**
   *
   * @return
   */
  public final float getJointMaxImpulse()
  {
    
    return parent.sizeX*parent.sizeY + parent.sizeY*parent.sizeZ + parent.sizeZ*parent.sizeX;
  }


  /**
   *
   * @return
   */
  public final float getMass()
  {
    return sizeX*sizeY*sizeZ*PhysicsConstants.BLOCK_DENSITY;
  }

  /**
   *
   * @param vec
   * @return
   */
  public static float min(Vector3f vec)
  {
    if (vec.x <= vec.y && vec.x <= vec.z) return vec.x;
    if (vec.y <= vec.x && vec.y <= vec.z) return vec.y;
    return vec.z;
  }

  /**
   *
   * @param vec
   * @return
   */
  public static float max(Vector3f vec)
  {
    if (vec.x >= vec.y && vec.x >= vec.z) return vec.x;
    if (vec.y >= vec.x && vec.y >= vec.z) return vec.y;
    return vec.z;
  }

  /**
   *
   * @param vec
   * @return
   */
  public static String vectorToStr(Vector3f vec)
  {
    return "("+vec.x +", "+vec.y +", "+vec.z +")";
  }

}