package com.haxademic.demo.draw.shapes.shader;

import com.haxademic.core.app.P;
import com.haxademic.core.app.PAppletHax;
import com.haxademic.core.constants.AppSettings;
import com.haxademic.core.draw.context.DrawUtil;
import com.haxademic.core.draw.shapes.PShapeUtil;
import com.haxademic.core.draw.textures.pgraphics.TextureEQConcentricCircles;
import com.haxademic.core.draw.textures.pgraphics.shared.BaseTexture;
import com.haxademic.core.draw.textures.pshader.TextureShader;
import com.haxademic.core.file.DemoAssets;
import com.haxademic.core.file.FileUtil;

import processing.core.PShape;
import processing.opengl.PShader;

public class Demo_VertexShader_PointsPShapeDeformAndColor 
extends PAppletHax {
	public static void main(String args[]) { PAppletHax.main(Thread.currentThread().getStackTrace()[1].getClassName()); }

	protected float modelHeight;
	protected PShape svg;
	protected float svgExtent;
	protected PShape obj;
	protected float objExtent;
	protected PShader pointsDeformAndTexture;
	protected BaseTexture audioTexture;
	protected TextureShader noiseTexture;

	protected void overridePropsFile() {
		int FRAMES = 320;
		p.appConfig.setProperty(AppSettings.LOOP_FRAMES, FRAMES);
//		p.appConfig.setProperty(AppSettings.RENDERING_MOVIE, true);
//		p.appConfig.setProperty(AppSettings.RENDERING_MOVIE_START_FRAME, 1 + FRAMES);
//		p.appConfig.setProperty(AppSettings.RENDERING_MOVIE_STOP_FRAME, 1 + FRAMES * 2);
	}
	
	protected void setupFirstFrame() {
		// mapped texture
//		audioTexture = new TextureEQGrid(300, 300);
		audioTexture = new TextureEQConcentricCircles(300, 300);
		
		// noise
		noiseTexture = new TextureShader(TextureShader.noise_simplex_2d_iq, 0.0005f);
		
		// create mesh shape
		svg = PShapeUtil.svgToUniformPointsShape(FileUtil.getFile("haxademic/svg/x.svg"), 5);
		svg = PShapeUtil.svgToUniformPointsShape(FileUtil.getFile("haxademic/svg/hexagon.svg"), 5);
		svg.disableStyle();
		PShapeUtil.centerShape(svg);
		PShapeUtil.scaleShapeToHeight(svg, p.height * 0.6f);
		PShapeUtil.addTextureUVSpherical(svg, audioTexture.texture());		// necessary for vertex shader `varying vertTexCoord`
		svgExtent = PShapeUtil.getMaxExtent(svg);
		// build obj PShape and scale to window
		obj = DemoAssets.objSkullRealistic();
		PShapeUtil.centerShape(obj);
		PShapeUtil.scaleShapeToHeight(obj, p.height * 0.7f);
		
		// replace with a points version
		obj = PShapeUtil.meshShapeToPointsShape(obj);
		obj.disableStyle();
		PShapeUtil.addTextureUVSpherical(obj, audioTexture.texture());		// necessary for vertex shader `varying vertTexCoord`
		objExtent = PShapeUtil.getMaxExtent(obj);
		
		// load shader to map texture via UV coords and displace with texture color
		pointsDeformAndTexture = p.loadShader(
			FileUtil.getFile("haxademic/shaders/point/point-frag.glsl"), 
			FileUtil.getFile("haxademic/shaders/point/particle-displace-texture-vert.glsl")
		);	
	}

	public void drawApp() {
		background(0);
		
		// update textures & switch between audio & noise
		audioTexture.update();
		if(p.mousePercentY() > 0.5f) {
			noiseTexture.shader().set("offset", 0f, P.p.frameCount * 0.005f);
			audioTexture.texture().filter(noiseTexture.shader());
		}
		
		// apply deform & texture shader
		pointsDeformAndTexture.set("colorMap", DemoAssets.textureNebula()); // audioTexture.texture()
		pointsDeformAndTexture.set("displacementMap", audioTexture.texture());
		pointsDeformAndTexture.set("displaceAmp", 1.5f);
		// change params per flat/3d model
		if(p.mousePercentX() > 0.5f) {
			pointsDeformAndTexture.set("modelMaxExtent", objExtent * 2f);
			pointsDeformAndTexture.set("sheet", 0);
		} else {
			pointsDeformAndTexture.set("modelMaxExtent", svgExtent * 2f);
			pointsDeformAndTexture.set("sheet", 1);
		}
		p.debugView.setTexture(audioTexture.texture());
		
		// rotate
		p.translate(p.width/2f, p.height/2f, 0);
		DrawUtil.basicCameraFromMouse(p.g);
		
		// draw points mesh 
		p.stroke(255);
//		p.strokeWeight(4);
		p.shader(pointsDeformAndTexture);
		if(p.mousePercentX() > 0.5f) {
			p.shape(obj);
		} else {
			p.shape(svg);
		}
		p.resetShader();
	}
		
}