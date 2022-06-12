package com.example.example6;

import static java.lang.System.exit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.w3c.dom.Text;

/**
 * Smart Phone Sensing Example 6. Object movement and interaction on canvas.
 */
public class MainActivity extends Activity implements OnClickListener, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor, directionSensor;

    private ShapeDrawable drawable;
    private Canvas canvas;
    private List<ShapeDrawable> walls;

    private List<Rectangle> obstacles = new ArrayList<>();

    private List<Particle> particles = new ArrayList<>();
    private List<Rectangle> building = new ArrayList<>();
    private List<Float> motionEvent = new ArrayList<>();
    private List<Float> motions = new ArrayList<>();

    private Timer timer = new Timer();

    private final int NUM_PART = 1000;
    private final double H = 10;

    private float ROTATION_OFFSET = -58;      // the buildings standard rotational offset
    private int TOTALSTEPS = 0;
    private final float STEP_SIZE = 0.6f;
    private final int PPM = 38;         // Pixels per meter
    private boolean INITROUND = true;


    float distance = 0;
    float direction = 0;

    int steps = 0;
    int firstnewMotionEvent = 0;
    private boolean blocking = false;
    // Measures motion in periods of 700ms
    TimerTask tt = new TimerTask() {
        @Override
        public void run() {
            copyMotionEvent();
            assessMotion();
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize sensormanager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }

        // get the screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        System.out.println("Size of screen known as: " + width + ", and height " + height);

        // create a drawable object
        drawBuilding(width,height);

        // create a canvas
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);

        generateParticles();

        for (Particle p : particles) {
            ShapeDrawable shape = new ShapeDrawable(new OvalShape());
            shape.setBounds((int)p.getX()-10, (int) p.getY()-10, (int) p.getX()+10, (int) p.getY()+10);
            shape.getPaint().setColor(Color.RED);
            shape.draw(canvas);
        }
        // draw the objects
        drawable.draw(canvas);
        for(ShapeDrawable wall : walls) {
            System.out.println("Drawing a wall");
            wall.draw(canvas);
        }
    }

    @Override
    public void onClick(View view) {
        // Do nothing.
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    // onResume() registers the sensors for listening the events
    protected void onResume() {
        super.onResume();

        // Set the sensors
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        directionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, directionSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Start motion measurement
        try {
            timer.schedule(tt, 700, 700);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    // onPause() unregisters the sensors for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, stepSensor);
        sensorManager.unregisterListener(this, directionSensor);
        System.out.println(steps + " steps total");
    }

    //method to hardcode all rooms within frame
    // pick 38 pixels per meter, based on 26,5 meters for the building, plus 2 meters for avoiding the screen border
    // 1080/(26,5+2)= 38 pixels/m
    // top border capped on 300 distance
    private void defineBuilding() {
        Rectangle room1 = new Rectangle(38,300,  138,120, 3); //room C3 center, 107, 360
        building.add(room1);
        Rectangle room2 = new Rectangle(38,420,  138,120, 2); //room C2 center, 107, 480
        building.add(room2);
        Rectangle room3 = new Rectangle(129, 540, 47,150, 1); //room C1 center: 152, 615
        building.add(room3);
        Rectangle room4 = new Rectangle(178, 540-162, 182,84,4 );//room C4 center: 269, 420
        building.add(room4);
        Rectangle room5 = new Rectangle(360, 540-162, 182,84,5);//room C5 center: 451, 420
        building.add(room5);
        Rectangle room6 = new Rectangle(542, 540-162, 182,84,6);// room C6 center: 633, 420
        building.add(room6);
        Rectangle room7 = new Rectangle(724, 540-162, 182,84,7);//room C7 center: 815, 420
        building.add(room7);
        Rectangle room8 = new Rectangle(906,300,  138,120,9);//room C9 center: 975, 360
        building.add(room8);
        Rectangle room9 = new Rectangle(906,420,  138,120,10);//room C10 center: 975, 480
        building.add(room9);
        Rectangle room10 = new Rectangle(906, 540, 47,150,11);//room C11 center: 929, 615
        building.add(room10);
        Rectangle room11 = new Rectangle(906-174, 690, 221,47,12);//room C12 center: 842, 713
        building.add(room11);
        Rectangle room12 = new Rectangle(906-164, 540-78, 164,78, 8);//room C8 center: 814, 501
        building.add(room12);
        Rectangle room13 = new Rectangle(580, 540-162+84, 87,164,13);//room C13 center: 623, 536//TODO: find exact X value
        building.add(room13);

        //Define small objects and walls between rooms to further define particles

        //Define walls splitting room 7 and 8, leaving the door opening accessible
        Rectangle topSideLeftWallRoom8 = new Rectangle(724,540-162,65,0,8);
        obstacles.add(topSideLeftWallRoom8);
        Rectangle topSideRightWallRoom8 = new Rectangle(906-70,540-162,70,0,8);
        obstacles.add(topSideRightWallRoom8);

        //Define wall between room 8 and room 10
        Rectangle rightSideRoom8 = new Rectangle(906,420,0,120,8);
        obstacles.add(rightSideRoom8);

        //Define walls splitting room 13 and 6, leaving the door opening accessible
        Rectangle topSideLeftWallRoom13 = new Rectangle(542, 540-162+84,80,0,0);
        obstacles.add(topSideLeftWallRoom13);
        Rectangle topSideRightWallRoom13 = new Rectangle(542+117,540-162+84,65,0,0);
        obstacles.add(topSideRightWallRoom13);

        //TODO: add border object between room 3 and 2

        //TODO: add border object between room 9 and 10


        System.out.println("Defined building " + building.size());
    }

    //Create extra walls/barriers/pillars to further strengthen

    private void drawBuilding(int width, int height) {
        walls = new ArrayList<>();
        drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.BLUE);
        drawable.setBounds(width/2-20, height/2-20, width/2+20, height/2+20);

        walls = new ArrayList<>();

        defineBuilding();

        for (int i = 0; i < building.size(); i++) {
            if (building.get(i) != null) {
                ShapeDrawable[] todraw = building.get(i).drawRectangle();
                for (int j = 0; j < todraw.length; j++) {
                    walls.add(todraw[j]);
                }
            }

        }

        System.out.println("Number of rooms is now:" + building.size());
        System.out.println("Size of walls is now: " + walls.size());
    }


    private boolean noCollision(Particle particle) {
        for (Rectangle wall : obstacles) {
            //walls have either 0 length or 0 width, otherwise they are rectangles
            assert wall.getWidth() == 0 || wall.getLength() == 0;
            if (wall.getWidth() == 0) {
//                System.out.println("found a vertical wall");
                if (movementCollision(particle,wall.getTopleftX(), wall.getTopleftY(),
                        wall.getTopleftX(), wall.getTopleftY() + wall.getLength())) {
                    return false;
                }
            }
            else {
//                System.out.println("found a horizontal wall");
                if (movementCollision(particle,wall.getTopleftX(), wall.getTopleftY(),
                        wall.getTopleftX() + wall.getWidth(), wall.getTopleftY())) {
                    return false;
                }
            }

        }
        return true;
    }

    //do some validity testing for a particle
    private boolean validParticle(Particle p) {
        if (building != null) {
            for (int i = 0; i < building.size(); i++) {
                Rectangle room = building.get(i);
                if (p.getX() > room.getTopleftX() && p.getX() < room.getTopleftX() + room.getWidth()) {
                    if (p.getY() > room.getTopleftY() && p.getY() < room.getTopleftY() + room.getLength()) {
//                        System.out.println("particle added: X " + p.getX() + " and Y " + p.getY());
//                        System.out.println("found in room " + i + ", coordinates " + room.getTopleftX() + ", " + room.getTopleftY());
                        if (noCollision(p)) {
                            return true;
                        }
                        else {
                            System.out.println("Detected collision");
                        }
                    }
                }
            }
        }
        return false;
    }

    private void generateParticles() {
        System.out.println("Generating particles");
        while(particles.size() < NUM_PART) {
            Particle part = new Particle(Math.random()*1080, Math.random()*2000, 1, Math.random()* 360);
            if (validParticle(part)) {
                particles.add(part);
            }
        }
    }

    private void updateParticles(double distance, double direction) {
        for (Particle p: particles) {
//            System.out.println("Current X and Y: " + p.getX() + ", " + p.getY());
            p.updateDistance(distance, direction);
//            System.out.println("New X and Y: " + p.getX() + ", " + p.getY());
        }

        //check which particles still live in a room, and remove those that are invalid
        List<Particle> toremove = new ArrayList<>();
        for (int i = 0; i < particles.size(); i++) {
            if (!validParticle(particles.get(i))) {
                toremove.add(particles.get(i));
            }
        }
        for (Particle p: toremove) {
            particles.remove(p);
        }

        System.out.println("Amount of particles still left is: " + particles.size());
        if (particles.size() == 0) {
            System.out.println("Empty particle set");
        }
        //use a CDF to allow weighted sampling over all points
        double[] cdf = cdfFromWeights();
        System.out.println("Cdf function now: " + Arrays.toString(cdf));
        while (particles.size() < NUM_PART) {
            double rand = Math.random();
            int kernel = 0; //kernel holds the points, X and Y, which we will use a mean for the the Guassian
            for (int i = 0; i < cdf.length; i++) {
                if (cdf[i] > rand) {
                    kernel = i;
                    break;
                }
            }
//            System.out.println("Found fitting kernel value: " + kernel);

            //apply Gaussian to set point nearby, using h as std dev and kernel X and Y as mean
            //idea used from: https://stats.stackexchange.com/questions/43674/simple-sampling-method-for-a-kernel-density-estimator
            double minX = Math.random();
            double minY = Math.random();

//            System.out.println("sample to check from: " + particles.get(kernel).getX() + ", "+ particles.get(kernel).getY());

            //Create new point by sampling from our Gaussian
            double newX = particles.get(kernel).getX() + Math.random() * H;
            double newY = particles.get(kernel).getY() + Math.random() * H;
            if (minX < 0.5) {//allow for left-side sampling by subtracting from the mean
                newX = particles.get(kernel).getX() - Math.random() * H;
            }
            if (minY < 0.5) {//allow for right-side sampling by adding to the mean
                newY = particles.get(kernel).getY() - Math.random() * H;
            }

//            System.out.println("new particle values X " + newX + ", and Y " + newY);

            //create a new particle
            Particle newP = new Particle(newX, newY, 1, Math.random()*360);
            if (validParticle(newP)) { //check if the particle is within one of our room and therefore valid
                particles.add(newP);
//                System.out.println("new Particle added");
            }
        }

        System.out.println("new particle size is now: " + particles.size());
    }

    public double[] cdfFromWeights() {
        float totalDist = 0;
        for (Particle p : particles) {
            totalDist += p.getDistance();
        }
        double[] x_coord = new double[particles.size()];
        double[] y_coord = new double[particles.size()];
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); i++) {
            x_coord[i] = particles.get(i).getX();
            y_coord[i] = particles.get(i).getY();
            weights[i] = particles.get(i).getDistance()/totalDist;
        }
        double[] cdf = new double[particles.size()];
        cdf[0] = weights[0];
        for (int i = 1; i < weights.length; i++) {
            cdf[i] = cdf[i-1] + weights[i];
        }
        return cdf;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int currSteps = 0;
        ImageView arrow = (ImageView) findViewById(R.id.arrowIcon);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                float RotationM[] = new float[9];
                float OrientationM[] = new float[3];        // Gives x, y, and z rotation
                SensorManager.getRotationMatrixFromVector(RotationM, event.values);
                SensorManager.getOrientation(RotationM, OrientationM);

                direction = (float)Math.toDegrees(OrientationM[0]) - ROTATION_OFFSET;
                float tempdir= direction;
                direction = clampDirection(direction);
                arrow.setRotation(direction);
                TextView room = (TextView) findViewById(R.id.roomText);
//                room.setText("Room: " + tempdir + "rounded" + direction);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                while( blocking ) {         // Blocks this thread to write motionEvent list to motions list
                    System.out.println("blocking = " + blocking);
                }
                motionEvent.add(event.values[2]);       // Only z value is used
        }
    }

    public float clampDirection(float direction) {
        float temp = direction;
        float count = 0;
        if (direction > 0) {
            while(temp > 45) {
                temp -= 45;
                count++;
            }
            if (count%2 == 0) {
                return direction-temp;
            }
            else {
                return direction- direction%45 + 45;
            }
        }
        else {
            while(temp < -45) {
                temp += 45;
                count++;
            }
            if (count%2 == 0) {
                return direction-temp;
            }
            else {
                return direction- direction%45 - 45;
            }
        }
    }

    public void copyMotionEvent(){
        blocking = true;
        for (int i = firstnewMotionEvent; i < motionEvent.size(); i++){
            motions.add(motionEvent.get(i));
        }
        firstnewMotionEvent = motionEvent.size();
        blocking = false;
    }

    public void assessMotion(){
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        float variance;
        for (float motion : motions){
            if (motion > max){
                max = motion;
            }
            if (motion < min){
                min = motion;
            }
        }
        variance = max - min;
        System.out.println("min = " + min + ", max = " + max + ", var = " + variance);
        if (variance >= 1.4){
            System.out.println("1 step");
            steps++;
            distance = 1 * STEP_SIZE * PPM;
            updateParticles(distance, direction);
            reDraw();
        } else {
            System.out.println("NO STEPS");
        }
        motions.clear();
    }

    // TODO: write in which room someone is (e.g. by deciding on the room with the most particles in it at any time)
    public void roomNumber(){
        TextView room = (TextView) findViewById(R.id.roomText);
        int roomNumber = 0;

        int[] roomCount = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        for (Particle p: particles) {
            for (int i = 0; i < building.size(); i++) {
                Rectangle rectangle = building.get(i);
                if (p.getX() > rectangle.getTopleftX() && p.getX() < rectangle.getTopleftX() + rectangle.getWidth()) {
                    if (p.getY() > rectangle.getTopleftY() && p.getY() < rectangle.getTopleftY() + rectangle.getLength()) {
//                        System.out.println("particle added: X " + p.getX() + " and Y " + p.getY());
//                        System.out.println("found in room " + i + ", coordinates " + room.getTopleftX() + ", " + room.getTopleftY());
                        roomCount[(int) rectangle.getRoom()] +=1;
                    }
                }
            }
        }
        int finalRoom = 0;
        int finalRoomCount = 0;
        for (int i = 0; i < roomCount.length; i++) {
            if (roomCount[i] > finalRoomCount) {
                finalRoom = i;
                finalRoomCount = roomCount[i];
            }
        }
        room.setText("Room is: " + finalRoom);
    }

    public void reDraw() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        System.out.println("Size of screen known as: " + width + ", and height " + height);

//        // create a drawable object
//        drawBuilding(width,height);

        // create a canvas
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);

        // redrawing of the object
        canvas.drawColor(Color.WHITE);
        drawable.draw(canvas);
        for(ShapeDrawable wall : walls) {
            wall.draw(canvas);
        }

        drawable.draw(canvas);
        for (Particle p : particles) {
            ShapeDrawable shape = new ShapeDrawable(new OvalShape());
            shape.setBounds((int)p.getX()-5, (int) p.getY()-5, (int) p.getX()+5, (int) p.getY()+5);
            shape.getPaint().setColor(Color.RED);
            if (p.getY() < 300) {
                shape.getPaint().setColor(Color.GREEN);
            }
            shape.draw(canvas);
        }
        roomNumber();
    }


    /**
     * Determines if the drawable dot intersects with any of the walls.
     * @return True if that's true, false otherwise.
     */
    private boolean isCollision() {
        for(ShapeDrawable wall : walls) {
            if(isCollision(wall,drawable))
                return true;
        }
        return false;
    }

    /**
     * Determines if two shapes intersect.
     * @param first The first shape.
     * @param second The second shape.
     * @return True if they intersect, false otherwise.
     */
    private boolean isCollision(ShapeDrawable first, ShapeDrawable second) {
        Rect firstRect = new Rect(first.getBounds());
        return firstRect.intersect(second.getBounds());
    }

    private boolean movementCollision(Particle p, double C_x, double C_y, double D_x, double D_y) {

        //Vertical wall
        if (C_x == D_x) {
            if (p.getY() > C_y && p.getY() < D_y && p.get_prev_Y() > C_y && p.get_prev_Y() < D_y) {
//                System.out.println("within proper Y range for collision");
                //within Y off wall
//                System.out.println("prev X " + p.get_prev_X() + ", new X " + p.getX() + ", age " +p.getDistance());
                if (p.get_prev_X() < C_x && p.getX() > C_x) {
                    System.out.println("Collision");
                    return true;
                }
                if  (p.get_prev_X() > C_x && p.getX() < C_x) {
                    System.out.println("Collision");
                    return true;
                }
            }
        }
        //Horizontal wall
        else {
            if(p.getX() > C_x && p.getX() < D_x && p.get_prev_X() > C_x && p.get_prev_X() < D_x) {
                //within X of wall
                if (p.get_prev_Y() < C_y && p.getY() > C_y) {
                    System.out.println("Collision");
                    return true;
                }
                if  (p.get_prev_Y() > C_y && p.getY() < C_y) {
                    System.out.println("Collision");
                    return true;
                }
            }
        }
        return false;
    }
}