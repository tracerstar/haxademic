package com.haxademic.sketch.test;

import com.haxademic.core.app.P;
import com.haxademic.core.app.PAppletHax;
import com.haxademic.core.app.config.AppSettings;
import com.haxademic.core.draw.shapes.PShapeUtil;
import com.haxademic.core.file.DemoAssets;

import processing.core.PShape;

public class LightsMaterialTest
extends PAppletHax {
	public static void main(String args[]) { PAppletHax.main(Thread.currentThread().getStackTrace()[1].getClassName()); }

	protected String emissiveMaterial = "emissiveMaterial";
	protected String ambientLight = "ambientLight";
	protected String specularMaterial = "specularMaterial";
	protected String specularLight = "specularLight";
	protected String shininessVal = "shininessVal";
	protected String lightsFalloffVal = "lightsFalloffVal";
	protected String lightsFalloffConstantVal = "lightsFalloffConstantVal";
	protected String spotLightConeAngle = "spotLightConeAngle";
	protected String spotLightConcentration = "spotLightConcentration";

	public float centerX;
	public float centerY;

	public float _frames = 100;
	public float _progress = 0;

	protected PShape obj;
	
	protected void overridePropsFile() {
		p.appConfig.setProperty( AppSettings.WIDTH, 800 );
		p.appConfig.setProperty( AppSettings.HEIGHT, 800 );
		p.appConfig.setProperty( AppSettings.RETINA, true );
		p.appConfig.setProperty( AppSettings.RENDERING_MOVIE, false );
		p.appConfig.setProperty( AppSettings.RENDERING_MOVIE_START_FRAME, 1001 );
		p.appConfig.setProperty( AppSettings.RENDERING_MOVIE_STOP_FRAME, Math.round(1001 + _frames-1) );
		p.appConfig.setProperty( AppSettings.SMOOTHING, AppSettings.SMOOTH_HIGH );
		p.appConfig.setProperty(AppSettings.SHOW_SLIDERS, true);
	}

	public void setup() {
		super.setup();
		
		p.sphereDetail(100);
		
		centerX = p.width/2;
		centerY = p.height/2;

		// controls
		p.ui.addSlider(emissiveMaterial, 5f, 0, 255, 0.5f, false);
		p.ui.addSlider(ambientLight, 30f, 0, 255, 0.5f, false);
		p.ui.addSlider(specularMaterial, 150, 0, 255, 0.5f, false);
		p.ui.addSlider(specularLight, 200, 0, 255, 0.5f, false);
		p.ui.addSlider(shininessVal, 50f, 0, 100, 0.5f, false);
		p.ui.addSlider(lightsFalloffVal, 0.002f, 0, 0.01f, 0.0001f, false);
		p.ui.addSlider(lightsFalloffConstantVal, 0.02f, 0, 2f, 0.01f, false);
		p.ui.addSlider(spotLightConeAngle, P.HALF_PI, 0, P.TWO_PI, 0.01f, false);
		p.ui.addSlider(spotLightConcentration, 100f, 0, 1000f, 1f, false);

		// load model
//		obj = p.loadShape( FileUtil.getFile("models/skull-realistic.obj"));
//		obj = p.loadShape( FileUtil.getFile("models/Trump_lowPoly.obj"));
		obj = DemoAssets.objSkullRealistic();
		PShapeUtil.scaleShapeToExtent(obj, p.height * 0.7f);
	}
	
	protected void addDirectionalLight() {
		// add global directional light
		float directionalOsc = _progress * P.TWO_PI;
		float pointX = centerX + centerX/2 * P.sin(P.PI + directionalOsc) ;
		float pointY = centerY + centerY/2 * P.cos(P.PI + directionalOsc) ;
		p.directionalLight(155, 135, 135, 2f * P.sin(directionalOsc), 2f * P.cos(directionalOsc), -1);
		// show debug light direction
		if(p.ui.active()) {
			p.pushMatrix();
			p.fill(255, 0, 0);
			p.translate(pointX, pointY, 0);
			p.sphere(10);
			p.popMatrix();
		}
	}
	
	protected void addPointLight() {
		// adds a non-directional light source
		float pointX = centerX + centerX/2 * P.sin(_progress * P.TWO_PI) ;
		p.pointLight(255, 102, 126, pointX, centerY, 0);
		// show debug light position
		if(p.ui.active()) {
			p.pushMatrix();
			p.fill(51, 102, 126);
			p.translate(pointX, p.height/2, 0);
			p.sphere(10);
			p.popMatrix();
		}
	}
	
	protected void addSpotLight() {
		// adds a directional, focusable light source
		float spotLightX = centerX + p.width * 0.1f;
		float spotLightY = centerX + p.width * 0.1f;
		p.spotLight(200, 255, 200, spotLightX, spotLightY, 100, 0, 0, -1, p.ui.value(spotLightConeAngle), p.ui.value(spotLightConcentration));
		// show debug light position
		if(p.ui.active()) {
			p.pushMatrix();
			p.fill(0, 255, 0);
			p.translate(spotLightX, centerY + p.height * 0.1f, 100);
			p.sphere(10);
			p.popMatrix();
		}
	}
	
	public void drawApp() {
		p.background(0);
		p.noStroke();
		p.pushMatrix();
		
		_progress = (p.frameCount % _frames) / _frames;
		
		
		////////////////////////////////
		// show debug lights
		////////////////////////////////
		addDirectionalLight();
		addPointLight();
		addSpotLight();

		////////////////////////////////
		// set lighting
		////////////////////////////////
		p.lightFalloff(p.ui.value(lightsFalloffConstantVal), p.ui.value(lightsFalloffVal), 0.0f);
		p.ambientLight(p.ui.value(ambientLight), p.ui.value(ambientLight), p.ui.value(ambientLight));
		p.lightSpecular(p.ui.value(specularLight), p.ui.value(specularLight), p.ui.value(specularLight));

		// materials:
		p.emissive(p.ui.value(emissiveMaterial), p.ui.value(emissiveMaterial), p.ui.value(emissiveMaterial));
		p.specular(p.ui.value(specularMaterial), p.ui.value(specularMaterial), p.ui.value(specularMaterial));
		p.shininess(p.ui.value(shininessVal));	// affects the specular blur


		////////////////////////////////
		// position & draw shapes grid
		////////////////////////////////
//		drawPrimitives();
		drawObj();

		// close context
		p.popMatrix();
		p.noLights();
	}
	
	protected void drawPrimitives() {
		p.pushMatrix();

		p.translate(p.width/2, p.height/2, -p.width);
		p.rotateX(p.mouseY * 0.01f);
		p.rotateY(p.mouseX * 0.01f);
		
		// build grid of primitives
		int rowSize = 4;
		int gridSize = 1000;
		int gridSizeHalf = gridSize/2;
		int spacing = gridSize / rowSize;
		int size = spacing/3;
		
		for (int x = 0; x < rowSize; x++) {
			for (int y = 0; y < rowSize; y++) {
				p.pushMatrix();
				p.translate(-gridSizeHalf + spacing/2 + x * spacing, -gridSizeHalf + spacing/2 + y * spacing, 0);
			
				p.fill(127);
				
				if(x % 2 == 0) {
					sphere(size);
				} else {
					box(size);
				}
				p.popMatrix();
			}
		}
		p.popMatrix();
	}
	
	protected void drawObj() {
		p.pushMatrix();
		p.translate(p.width/2, p.height * 0.45f, -p.width);
		p.rotateY(P.sin(P.PI + P.TWO_PI * _progress) * 0.5f);
//		obj.disableStyle();
		p.fill(70);
		p.shape(obj);
		p.popMatrix();
	}
	
}
