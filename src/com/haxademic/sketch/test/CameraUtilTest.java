package com.haxademic.sketch.test;

import javax.media.opengl.GL2;

import com.haxademic.core.app.PAppletHax;
import com.haxademic.core.cameras.CameraUtil;
import com.haxademic.core.image.PerlinTexture;

import controlP5.ControlP5;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

@SuppressWarnings("serial")
public class CameraUtilTest
extends PAppletHax {

	protected PerlinTexture _perlinTexture;
	
	public float fogStart = 0;
	public float fogEnd = 0;
	public float cameraNear = 0;
	public float cameraDist = 0;
	protected ControlP5 _cp5;
	
	PGraphicsOpenGL pg; 
	PGL pgl;
	GL2 gl;


	protected void overridePropsFile() {
		_appConfig.setProperty( "width", "800" );
		_appConfig.setProperty( "height", "600" );
	}

	public void setup() {
		super.setup();	

		_cp5 = new ControlP5(this);
		_cp5.addSlider("fogStart").setPosition(20,20).setWidth(100).setRange(0, 10000).setValue(0);
		_cp5.addSlider("fogEnd").setPosition(20,40).setWidth(100).setRange(0, 10000).setValue(10000);
		_cp5.addSlider("cameraNear").setPosition(20,60).setWidth(100).setRange(0, 1000).setValue(1);
		_cp5.addSlider("cameraDist").setPosition(20,80).setWidth(100).setRange(0, 100000).setValue(100000);
	}

	public void drawApp() {
		p.background(0);
		
		  pg = (PGraphicsOpenGL)g;
		  pgl = beginPGL();  
		  gl = ((PJOGL)pgl).gl.getGL2();
		  
		  CameraUtil.setCameraDistance(p.g, cameraNear, cameraDist);

		p.fill(255);
		if((p.frameCount/10)%2 == 0) {
			p.ortho();
//			OpenGLUtil.setFog(p.g, true);
			
			gl.glEnable(GL2.GL_FOG);
		    float[] Fog_colour = {0,0,1f,1f};
		    gl.glHint(GL2.GL_FOG_HINT, GL2.GL_NICEST);
		    //gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
		    gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
		    //gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
		    gl.glFogf(GL2.GL_FOG_DENSITY, 0.005f);
		    gl.glFogfv(GL2.GL_FOG_COLOR, Fog_colour, 0);
		    gl.glFogf(GL2.GL_FOG_START, fogStart);
		    gl.glFogf(GL2.GL_FOG_END, fogEnd);

			
			p.text("fogging", 40, 40);
		} else {
			gl.glDisable(GL2.GL_FOG);
		}		
		
		p.pushMatrix();
		
		translate(p.width/2, p.height/2);
		p.fill(255, 0, 255);
		p.stroke(255);
		
		for(int z = 0; z > -20000; z -= 100) {
			p.translate(40, -40, -100);
			p.box(100 + -z/100);
		}
		p.popMatrix();
		p.endPGL();

	}
}
