package com.marginallyclever.gcodesender.Generators;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.marginallyclever.gcodesender.GcodeSender;


// source http://introcs.cs.princeton.edu/java/32class/Hilbert.java.html
public class HilbertCurveGenerator implements GcodeGenerator {
	private static final String G90_NL = "G90\n";
	private float turtleDx;
	private float turtleDy;
	private float turtleStep =10.0f;
	private float xmax = 7;
	private float xmin = -7;
	private float ymax = 7;
	private float ymin = -7;
	private final float toolOffsetZ = 1.25f;
	private float zDown = 40;
	private float zUp =90;
	private int order=4; // controls complexity of curve

	
	public String GetMenuName() {
		return "Hilbert Curve";
	}
	
	
	public void Generate() {
		final JDialog driver = new JDialog(GcodeSender.getSingleton().GetMainFrame(),"Your Message Here",true);
		driver.setLayout(new GridLayout(0,1));

		final JTextField fieldSize = new JTextField(Integer.toString((int)xmax));
		final JTextField fieldOrder = new JTextField(Integer.toString(order));
		final JTextField fieldUp = new JTextField(Integer.toString((int) zUp));
		final JTextField fieldDown = new JTextField(Integer.toString((int) zDown));

		driver.add(new JLabel("Size"));		driver.add(fieldSize);
		driver.add(new JLabel("Order"));	driver.add(fieldOrder);
		driver.add(new JLabel("Up"));		driver.add(fieldUp);
		driver.add(new JLabel("Down"));		driver.add(fieldDown);

		final JButton buttonSave = new JButton("Go");
		final JButton buttonCancel = new JButton("Cancel");
		Box horizontalBox = Box.createHorizontalBox();
	    horizontalBox.add(Box.createGlue());
	    horizontalBox.add(buttonSave);
	    horizontalBox.add(buttonCancel);
	    driver.add(horizontalBox);
		
		ActionListener driveButtons = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object subject = e.getSource();
				
				if(subject == buttonSave) {
					zUp = Float.parseFloat(fieldUp.getText());
					zDown = Float.parseFloat(fieldDown.getText());
					xmax = Integer.parseInt(fieldSize.getText());
					ymax= xmax;
					xmin=-xmax;
					ymin=-xmax;
					order = Integer.parseInt(fieldOrder.getText());
					CreateCurveNow();
					
					driver.dispose();
				}
				if(subject == buttonCancel) {
					driver.dispose();
				}
			}
		};
		
		buttonSave.addActionListener(driveButtons);
		buttonCancel.addActionListener(driveButtons);

		driver.setSize(300,400);
		driver.setVisible(true);
	}
	

	private void CreateCurveNow() {
		try {
			String outputFile = System.getProperty("user.dir") + "/" + "TEMP.NGC";
			System.out.println("output file = "+outputFile);
			OutputStream output = new FileOutputStream(outputFile);
			output.write("G28\n".getBytes(StandardCharsets.UTF_8));
			output.write(G90_NL.getBytes(StandardCharsets.UTF_8));
			output.write(("G54 X-30 Z-"+ toolOffsetZ +"\n").getBytes(StandardCharsets.UTF_8));
			
			turtleDx =0;
			turtleDy =-1;
			turtleStep = (float)((xmax-xmin) / (Math.pow(2, order)));

			// Draw bounding box
			output.write(G90_NL.getBytes(StandardCharsets.UTF_8));
			output.write(("G0 Z"+ zUp +"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+xmax+" Y"+ymax+"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 Z"+ zDown +"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+xmax+" Y"+ymin+"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+xmin+" Y"+ymin+"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+xmin+" Y"+ymax+"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+xmax+" Y"+ymax+"\n").getBytes(StandardCharsets.UTF_8));
			output.write(("G0 Z"+ zUp +"\n").getBytes(StandardCharsets.UTF_8));

			// move to starting position
			output.write("G91\n".getBytes(StandardCharsets.UTF_8));
			output.write(("G0 X"+(-turtleStep /2)+" Y"+(-turtleStep /2)+"\n").getBytes(StandardCharsets.UTF_8));
						
			// do the curve
			output.write(G90_NL.getBytes(StandardCharsets.UTF_8));
			output.write(("G0 Z"+ zDown +"\n").getBytes(StandardCharsets.UTF_8));
			
			output.write("G91\n".getBytes(StandardCharsets.UTF_8));
			hilbert(output,order);
			
			output.write(G90_NL.getBytes(StandardCharsets.UTF_8));
			output.write(("G0 Z"+ zUp +"\n").getBytes(StandardCharsets.UTF_8));

			// finish up
			output.write("G28\n".getBytes(StandardCharsets.UTF_8));
			
        	output.flush();
	        output.close();
	        
			// open the file automatically to save a click.
			GcodeSender.getSingleton().openFile(outputFile);
		}
		catch(IOException ex) {}
	}
	
	
    // Hilbert curve
    private void hilbert(OutputStream output,int n) throws IOException {
        if (n == 0) return;
        turtle_turn(90);
        treblih(output,n-1);
        turtle_goForward(output);
        turtle_turn(-90);
        hilbert(output,n-1);
        turtle_goForward(output);
        hilbert(output,n-1);
        turtle_turn(-90);
        turtle_goForward(output);
        treblih(output,n-1);
        turtle_turn(90);
    }


    // evruc trebliH
    public void treblih(OutputStream output,int n) throws IOException {
        if (n == 0) return;
        turtle_turn(-90);
        hilbert(output,n-1);
        turtle_goForward(output);
        turtle_turn(90);
        treblih(output,n-1);
        turtle_goForward(output);
        treblih(output,n-1);
        turtle_turn(90);
        turtle_goForward(output);
        hilbert(output,n-1);
        turtle_turn(-90);
    }
    

    public void turtle_turn(float degrees) {
    	double n = degrees * Math.PI / 180.0;
    	double newx =  Math.cos(n) * turtleDx + Math.sin(n) * turtleDy;
    	double newy = -Math.sin(n) * turtleDx + Math.cos(n) * turtleDy;
    	double len = Math.sqrt(newx*newx + newy*newy);
    	assert(len>0);
    	turtleDx = (float)(newx/len);
    	turtleDy = (float)(newy/len);
    }

    
    public void turtle_goForward(OutputStream output) throws IOException {
		output.write(("G0 X"+(turtleDx * turtleStep)+" Y"+(turtleDy * turtleStep)+"\n").getBytes(StandardCharsets.UTF_8));
    }
}
