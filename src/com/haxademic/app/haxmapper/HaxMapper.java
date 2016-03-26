package com.haxademic.app.haxmapper;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

import com.haxademic.app.haxmapper.distribution.MappingGroup;
import com.haxademic.app.haxmapper.dmxlights.RandomLightTiming;
import com.haxademic.app.haxmapper.overlays.FullMaskTextureOverlay;
import com.haxademic.app.haxmapper.overlays.MeshLines.MODE;
import com.haxademic.app.haxmapper.polygons.IMappedPolygon;
import com.haxademic.app.haxmapper.polygons.MappedQuad;
import com.haxademic.app.haxmapper.polygons.MappedTriangle;
import com.haxademic.app.haxmapper.textures.BaseTexture;
import com.haxademic.app.haxmapper.textures.TextureKinectFacePlayback;
import com.haxademic.app.haxmapper.textures.TextureKinectFaceRecording;
import com.haxademic.app.haxmapper.textures.TextureVideoPlayer;
import com.haxademic.core.app.P;
import com.haxademic.core.app.PAppletHax;
import com.haxademic.core.data.ConvertUtil;
import com.haxademic.core.draw.util.DrawUtil;
import com.haxademic.core.draw.util.OpenGLUtil;
import com.haxademic.core.draw.util.OpenGLUtil.Blend;
import com.haxademic.core.hardware.midi.AbletonNotes;
import com.haxademic.core.hardware.midi.AkaiMpdPads;
import com.haxademic.core.hardware.osc.TouchOscPads;
import com.haxademic.core.hardware.shared.InputTrigger;
import com.haxademic.core.image.filters.shaders.BadTVLinesFilter;
import com.haxademic.core.image.filters.shaders.BrightnessFilter;
import com.haxademic.core.image.filters.shaders.ColorDistortionFilter;
import com.haxademic.core.image.filters.shaders.ContrastFilter;
import com.haxademic.core.image.filters.shaders.CubicLensDistortionFilter;
import com.haxademic.core.image.filters.shaders.DeformBloomFilter;
import com.haxademic.core.image.filters.shaders.DeformTunnelFanFilter;
import com.haxademic.core.image.filters.shaders.EdgesFilter;
import com.haxademic.core.image.filters.shaders.FXAAFilter;
import com.haxademic.core.image.filters.shaders.HalftoneFilter;
import com.haxademic.core.image.filters.shaders.HueFilter;
import com.haxademic.core.image.filters.shaders.InvertFilter;
import com.haxademic.core.image.filters.shaders.KaleidoFilter;
import com.haxademic.core.image.filters.shaders.MirrorFilter;
import com.haxademic.core.image.filters.shaders.PixelateFilter;
import com.haxademic.core.image.filters.shaders.RadialRipplesFilter;
import com.haxademic.core.image.filters.shaders.SphereDistortionFilter;
import com.haxademic.core.image.filters.shaders.WobbleFilter;
import com.haxademic.core.math.MathUtil;
import com.haxademic.sketch.hardware.kinect_openni.KinectFaceRecorder;

import oscP5.OscMessage;
import processing.core.PConstants;
import processing.core.PGraphics;


@SuppressWarnings("serial")
public class HaxMapper
extends PAppletHax {
		
	// mesh polygons & graphics layers
	protected String _inputFileLines[];
	protected Rectangle _boundingBox;
	protected float[] extentsX = {-1,-1};
	protected float[] extentsY = {-1,-1};
	protected ArrayList<MappingGroup> _mappingGroups;
	protected PGraphics _overlayPG;
	protected PGraphics _fullMask;
	protected PGraphics _fullMaskOverlay;
	protected FullMaskTextureOverlay _fullMaskTexture;
	
	// texture pool
	public static int MAX_ACTIVE_TEXTURES = 4;
	public static int MAX_ACTIVE_TEXTURES_PER_GROUP = 2;
	public static int MAX_ACTIVE_MOVIE_TEXTURES = 2;
	protected ArrayList<BaseTexture> _texturePool;
	protected ArrayList<BaseTexture> _curTexturePool;
	protected ArrayList<BaseTexture> _movieTexturePool;
	protected ArrayList<BaseTexture> _activeTextures;
	protected ArrayList<BaseTexture> _overlayTexturePool;
	protected int _texturePoolNextIndex = 0;
	protected boolean _debugTextures = false;

	// user input triggers
	protected InputTrigger _colorTrigger = new InputTrigger(new char[]{'c'},new String[]{TouchOscPads.PAD_01},new Integer[]{AkaiMpdPads.PAD_01, AbletonNotes.NOTE_01});
	protected InputTrigger _rotationTrigger = new InputTrigger(new char[]{'v'},new String[]{TouchOscPads.PAD_02},new Integer[]{AkaiMpdPads.PAD_02, AbletonNotes.NOTE_02});
	protected InputTrigger _timingTrigger = new InputTrigger(new char[]{'n'},new String[]{TouchOscPads.PAD_03},new Integer[]{AkaiMpdPads.PAD_03, AbletonNotes.NOTE_03});
	protected InputTrigger _modeTrigger = new InputTrigger(new char[]{'m'},new String[]{TouchOscPads.PAD_04},new Integer[]{AkaiMpdPads.PAD_04, AbletonNotes.NOTE_04});
	protected InputTrigger _timingSectionTrigger = new InputTrigger(new char[]{'f'},new String[]{TouchOscPads.PAD_05},new Integer[]{AkaiMpdPads.PAD_05, AbletonNotes.NOTE_05});
	protected InputTrigger _newTextureTrigger = new InputTrigger(new char[]{'b'},new String[]{TouchOscPads.PAD_09},new Integer[]{AkaiMpdPads.PAD_09, AbletonNotes.NOTE_09});
	protected InputTrigger _allSameTextureTrigger = new InputTrigger(new char[]{'a'},new String[]{TouchOscPads.PAD_06},new Integer[]{AkaiMpdPads.PAD_06, AbletonNotes.NOTE_06});
	protected InputTrigger _bigChangeTrigger = new InputTrigger(new char[]{' '},new String[]{TouchOscPads.PAD_07},new Integer[]{AkaiMpdPads.PAD_07, AbletonNotes.NOTE_07});
	protected InputTrigger _lineModeTrigger = new InputTrigger(new char[]{'l'},new String[]{TouchOscPads.PAD_08},new Integer[]{AkaiMpdPads.PAD_08, AbletonNotes.NOTE_08});
	protected InputTrigger _audioInputUpTrigger = new InputTrigger(new char[]{},new String[]{"/7/nav1"},new Integer[]{});
	protected InputTrigger _audioInputDownTrigger = new InputTrigger(new char[]{},new String[]{"/7/nav2"},new Integer[]{});
	protected InputTrigger _brightnessUpTrigger = new InputTrigger(new char[]{']'},new String[]{},new Integer[]{});
	protected InputTrigger _brightnessDownTrigger = new InputTrigger(new char[]{'['},new String[]{},new Integer[]{});
	protected InputTrigger _debugTexturesTrigger = new InputTrigger(new char[]{'d'},new String[]{},new Integer[]{});
	protected int _lastInputMillis = 0;
	protected int USER_INPUT_BEAT_TIMEOUT = 5000;
	
	// beat-detection trigger intervals
	protected float BEAT_DIVISOR = 1f; // 10 to test
	protected int BEAT_INTERVAL_COLOR = (int) Math.ceil(6f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_MAP_STYLE_CHANGE = (int) Math.ceil(4f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_ROTATION = (int) Math.ceil(16f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_TRAVERSE = (int) Math.ceil(20f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_ALL_SAME = (int) Math.ceil(140f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_LINE_MODE = (int) Math.ceil(32f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_NEW_TIMING = (int) Math.ceil(40f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_NEW_TEXTURE = (int) Math.ceil(80f / BEAT_DIVISOR);
	protected int BEAT_INTERVAL_BIG_CHANGE = (int) Math.ceil(250f / BEAT_DIVISOR);
	protected boolean _timingDebug = false;
	protected int numBeatsDetected = 0;
	
	// face recorder insanity mode
	protected KinectFaceRecorder _faceRecorder;
	protected BaseTexture _faceRecordingTexture;
	protected BaseTexture _facesPlaybackTexture;

	// dmx physical lighting
	protected RandomLightTiming _dmxLights;

	// global effects processing
	protected int[] _textureEffectsIndices = {0,0,0,0,0,0,0};	// store a effects number for each texture position after the first
	protected int _numTextureEffects = 16 + 8; // +8 to give a good chance at removing the filter from the texture slot
	
	public void oscEvent(OscMessage theOscMessage) {  
		super.oscEvent(theOscMessage);
		String oscMsg = theOscMessage.addrPattern();
		// handle brightness slider
		if( oscMsg.indexOf("/7/fader0") != -1) {
//			_brightnessVal = theOscMessage.get(0).floatValue() * 3.0f;
		}		
	}

	protected void overridePropsFile() {
		_appConfig.setProperty( "rendering", "false" );
		_appConfig.setProperty( "fullscreen", "true" );
		_appConfig.setProperty( "fills_screen", "true" );
		_appConfig.setProperty( "kinect_active", "false" );
		_appConfig.setProperty( "osc_active", "true" );
	}
	
	/////////////////////////////////////////////////////////////////
	// Setup: build mapped polygon groups & init texture pools
	/////////////////////////////////////////////////////////////////

	public void setup() {
		super.setup();
//		p.smooth(OpenGLUtil.SMOOTH_MEDIUM);
		p.noSmooth();
		noStroke();
		importPolygons();
		for( int i=0; i < _mappingGroups.size(); i++ ) _mappingGroups.get(i).completePolygonImport();
		buildTextures();
		if(p.appConfig.getInt("dmx_lights_count", 0) > 0) _dmxLights = new RandomLightTiming(p.appConfig.getInt("dmx_lights_count", 0));
	}
	
	protected void importPolygons() {
		_boundingBox = new Rectangle(-1, -1, 0, 0);
		
		_overlayPG = P.p.createGraphics( p.width, p.height, PConstants.OPENGL );
//		_overlayPG.smooth(OpenGLUtil.SMOOTH_MEDIUM);
		_overlayPG.noSmooth();
		_mappingGroups = new ArrayList<MappingGroup>();
		
		if( _appConfig.getString("mapping_file", "") == "" ) {
			_mappingGroups.add( new MappingGroup( this, _overlayPG, _boundingBox) );
			for(int i=0; i < 200; i++ ) {
				// create triangle
				float startX = p.random(0,p.width);
				float startY = p.random(0,p.height);
				float x2 = startX + p.random(-300,300);
				float y2 = startY + p.random(-300,300);
				// float x3 = startX + p.random(-300,300);
				float x3 = startY + p.random(-300,300);
				float y3 = startY + p.random(-300,300);
				// add polygon
				_mappingGroups.get(0).addPolygon( new MappedTriangle( startX, startY, x2, y2, x3, y3 ) );
				// update bounding box as we build
				updateBoundingBox(startX, startY);
				updateBoundingBox(x2, y2);
				updateBoundingBox(x3, y3);
			}
			_mappingGroups.get(0).addPolygon( new MappedTriangle( 100, 200, 400, 700, 650, 300 ) );
		} else {
			_inputFileLines = loadStrings(_appConfig.getString("mapping_file", ""));
			for( int i=0; i < _inputFileLines.length; i++ ) {
				String inputLine = _inputFileLines[i]; 
				// count lines that contain characters
				if( inputLine.indexOf("#group#") != -1 ) {
					_mappingGroups.add( new MappingGroup( this, _overlayPG, _boundingBox ) );
				} else if( inputLine.indexOf("#poly#") != -1 ) {
					// poly!
					inputLine = inputLine.replace("#poly#", "");
					String polyPoints[] = inputLine.split(",");
					if(polyPoints.length == 6) {
						// add polygons
						_mappingGroups.get(_mappingGroups.size()-1).addPolygon( new MappedTriangle( 
								ConvertUtil.stringToFloat( polyPoints[0] ), 
								ConvertUtil.stringToFloat( polyPoints[1] ), 
								ConvertUtil.stringToFloat( polyPoints[2] ), 
								ConvertUtil.stringToFloat( polyPoints[3] ), 
								ConvertUtil.stringToFloat( polyPoints[4] ), 
								ConvertUtil.stringToFloat( polyPoints[5] )
						) );
						// update bounding box as we build
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[0] ), ConvertUtil.stringToFloat( polyPoints[1] ));
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[2] ), ConvertUtil.stringToFloat( polyPoints[3] ));
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[4] ), ConvertUtil.stringToFloat( polyPoints[5] ));

					} else if(polyPoints.length == 8) {
						// add polygons
						_mappingGroups.get(_mappingGroups.size()-1).addPolygon( new MappedQuad( 
								ConvertUtil.stringToFloat( polyPoints[0] ), 
								ConvertUtil.stringToFloat( polyPoints[1] ), 
								ConvertUtil.stringToFloat( polyPoints[2] ), 
								ConvertUtil.stringToFloat( polyPoints[3] ), 
								ConvertUtil.stringToFloat( polyPoints[4] ), 
								ConvertUtil.stringToFloat( polyPoints[5] ),
								ConvertUtil.stringToFloat( polyPoints[6] ), 
								ConvertUtil.stringToFloat( polyPoints[7] )
						) );
						// update bounding box as we build
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[0] ), ConvertUtil.stringToFloat( polyPoints[1] ));
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[2] ), ConvertUtil.stringToFloat( polyPoints[3] ));
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[4] ), ConvertUtil.stringToFloat( polyPoints[5] ));
						updateBoundingBox(ConvertUtil.stringToFloat( polyPoints[6] ), ConvertUtil.stringToFloat( polyPoints[7] ));
					}
				}  
			}
		}
	}
	
	protected void buildOverlayMask() {
		// draw black on white
		_fullMask = p.createGraphics(p.width, p.height, P.P2D);
		_fullMask.smooth(OpenGLUtil.SMOOTH_HIGH);
		_fullMask.beginDraw();
		_fullMask.background(255);
		_fullMask.fill(0);
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).drawShapeForMask(_fullMask);
		}
		_fullMask.endDraw();
		
		// crate simple black overlay with polygons excluded via masking 
		_fullMaskOverlay = p.createGraphics(p.width, p.height, P.P2D);
		_fullMaskOverlay.smooth(OpenGLUtil.SMOOTH_HIGH);
		_fullMaskOverlay.beginDraw();
		_fullMaskOverlay.background(0);
		_fullMaskOverlay.endDraw();
		_fullMaskOverlay.mask(_fullMask);
	}
	
	protected void updateBoundingBox(float x, float y) {
		if(x < extentsX[0] || extentsX[0] == -1) extentsX[0] = x;
		if(x > extentsX[1] || extentsX[1] == -1) extentsX[1] = x;
		if(y < extentsY[0] || extentsY[0] == -1) extentsY[0] = y;
		if(y > extentsY[1] || extentsY[1] == -1) extentsY[1] = y;
		_boundingBox.x = (int) Math.floor(extentsX[0]);
		_boundingBox.width = (int) Math.ceil(extentsX[1] - extentsX[0]);
		_boundingBox.y = (int) Math.floor(extentsY[0]);
		_boundingBox.height= (int) Math.ceil(extentsY[1] - extentsY[0]);
	}
	
	protected void buildTextures() {
		if(p.appConfig.getBoolean("kinect_active", false ) == true) {
			_faceRecorder = new KinectFaceRecorder(this);
			_faceRecordingTexture = new TextureKinectFaceRecording(320, 240);
			_facesPlaybackTexture = new TextureKinectFacePlayback(320, 240);
		}
		
		_texturePool = new ArrayList<BaseTexture>();
		_curTexturePool = new ArrayList<BaseTexture>();
		_movieTexturePool = new ArrayList<BaseTexture>();
		_activeTextures = new ArrayList<BaseTexture>();
		_overlayTexturePool = new ArrayList<BaseTexture>();
		_fullMaskTexture = new FullMaskTextureOverlay(_overlayPG, _boundingBox);
		addTexturesToPool();
		storeVideoTextures();
		buildMappingGroups();
	}
			
	protected void buildMappingGroups() {
		// override this!
	}
	
	protected void addTexturesToPool() {
		// override this!
	}
	
	protected void storeVideoTextures() {
		// store just movies to restrain the number of concurrent movies
		for( int i=0; i < _texturePool.size(); i++ ) {
			if( _texturePool.get(i) instanceof TextureVideoPlayer ) {
				_movieTexturePool.add( _texturePool.get(i) );
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// Main draw loop
	/////////////////////////////////////////////////////////////////

	public void drawApp() {
//		prepareOverlayGraphics();
		p.blendMode(P.BLEND);
		background(0);
		if(_faceRecorder != null) updateFaceRecorder();
		checkBeat();
		updateActiveTextures();
		filterActiveTextures();
		drawMappingGroups();
		drawOverlays();
		postProcessFilters();
		drawOverlayMask();
		runDmxLights();
		if(_debugTextures == true) debugTextures();
	}
	
	protected void drawMappingGroups() {
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).draw();
		}
	}
	
	protected void drawOverlays() {
		// let current overlay texture update before ma
		_overlayTexturePool.get(0).update();
		
		// draw mesh & overlay on top
		_overlayPG.beginDraw();
		_overlayPG.clear();
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).drawOverlay();
		}
		// draw semi-transparent current texture on top
		if(_fullMaskTexture != null) _fullMaskTexture.drawOverlay();
		_overlayPG.endDraw();
		
		// draw composited overlay buffer
		DrawUtil.setColorForPImage(p);
		DrawUtil.resetPImageAlpha(p);
		OpenGLUtil.setBlendMode(p.g, Blend.ADDITIVE);
//		p.blendMode(P.ADD);
//		p.blendMode(P.SCREEN);
		p.image( _overlayPG, 0, 0, _overlayPG.width, _overlayPG.height );
		OpenGLUtil.setBlendMode(p.g, Blend.DEFAULT);
//		p.blendMode(P.BLEND);
	}
	
	protected void drawOverlayMask() {
		if(p.frameCount == 1) buildOverlayMask();
		p.image( _fullMaskOverlay, 0, 0, _fullMaskOverlay.width, _fullMaskOverlay.height );
	}

	protected void debugTextures() {
		// debug current textures
		for( int i=0; i < _activeTextures.size(); i++ ) {
			p.image(_activeTextures.get(i).texture(), i * 100, p.height - 100, 100, 100);
		}
	}
	
	protected void runDmxLights() {
		if(_dmxLights != null) {
			_dmxLights.update();
			if(_debugTextures == true) _dmxLights.drawDebug(p.g);
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// Texture-cycling methods
	/////////////////////////////////////////////////////////////////
	
	protected void shuffleTexturePool() {
		Collections.shuffle(_texturePool);
	}
	
	protected BaseTexture randomActiveTexture() {
		return _activeTextures.get( MathUtil.randRange(0, _activeTextures.size()-1) );
	}
	
	protected BaseTexture randomCurTexture() {
		return _curTexturePool.get( MathUtil.randRange(0, _curTexturePool.size()-1) );
	}
	
	protected MappingGroup getRandomGroup() {
		return _mappingGroups.get( MathUtil.randRange( 0, _mappingGroups.size()-1) );
	}
	
	protected int nextTexturePoolIndex() {
		_texturePoolNextIndex++;
		if(_texturePoolNextIndex >= _texturePool.size()) {
			_texturePoolNextIndex = 0;
			shuffleTexturePool(); // shuffle texture pool array again to prevent the same combination over and over
		}
		return _texturePoolNextIndex;
	}

	protected void nextOverlayTexture() {
		// cycle the first to the last element & set on fullMaskTexture 
		_overlayTexturePool.add(_overlayTexturePool.remove(0));
		_fullMaskTexture.setTexture(_overlayTexturePool.get(0));
	}

	protected void updateActiveTextures() {
		// reset active texture pool array
		while( _activeTextures.size() > 0 ) {
			_activeTextures.remove( _activeTextures.size() - 1 ).resetUseCount();
		}
		// figure out which textures are being used and rebuild array, telling active textures that they're active
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			ArrayList<BaseTexture> textures = _mappingGroups.get(i).textures();
			for( int j=0; j < textures.size(); j++ ) {
				if( _activeTextures.indexOf( textures.get(j) ) == -1 ) {
					if(textures.get(j).isActive() == false) P.println(textures.get(j).toString());
					textures.get(j).setActive(true);
					_activeTextures.add( textures.get(j) );
				}
			}
		}
		// set inactive pool textures' _active state to false (mostly for turning off video players)
		for( int i=0; i < _texturePool.size(); i++ ) {
			if( _texturePool.get(i).useCount() == 0 && _texturePool.get(i).isActive() == true ) {
				_texturePool.get(i).setActive(false);
				// P.println("Deactivated: ", _texturePool.get(i).getClass().getName());
			}
		}
		// update active textures, once each
		for( int i=0; i < _activeTextures.size(); i++ ) {
			_activeTextures.get(i).update();
		}
//		P.println(_activeTextures.size());
	}
	
	protected int numMovieTextures() {
		int numMovieTextures = 0;
		for( int i=0; i < _curTexturePool.size(); i++ ) {
			if( _curTexturePool.get(i) instanceof TextureVideoPlayer ) numMovieTextures++;
		}
		return numMovieTextures;
	}
	
	protected void removeOldestMovieTexture() {
		for( int i=0; i < _curTexturePool.size(); i++ ) {
			if( _curTexturePool.get(i) instanceof TextureVideoPlayer ) {
				_curTexturePool.remove(i);
				return;
			}
		}
	}
	
	protected void cycleANewTexture(BaseTexture specificTexture) {
		// rebuild the array of currently-available textures
		// check number of movie textures, and make sure we never have more than 2
		if(specificTexture != null) {
			_curTexturePool.add(specificTexture);
		} else {
			_curTexturePool.add( _texturePool.get( nextTexturePoolIndex() ) );
		}
		while( numMovieTextures() >= MAX_ACTIVE_MOVIE_TEXTURES ) {
			removeOldestMovieTexture();
			_curTexturePool.add( _texturePool.get( nextTexturePoolIndex() ) );
		}
		// remove oldest texture if more than max 
		if( _curTexturePool.size() >= MAX_ACTIVE_TEXTURES ) {
			// P.println(_curTexturePool.size());
			_curTexturePool.remove(0);
		}
		
		refreshGroupsTextures();
	}
	
	protected void refreshGroupsTextures() {
		// make sure polygons update their textures
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).shiftTexture();
			_mappingGroups.get(i).pushTexture( randomCurTexture() );
			_mappingGroups.get(i).refreshActiveTextures();				
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// Texture-level post-processing effects
	/////////////////////////////////////////////////////////////////

	protected void selectNewActiveTextureFilters() {
		for(int i=1; i < _textureEffectsIndices.length; i++) {
			if(MathUtil.randRange(0, 10) > 8) {
				_textureEffectsIndices[i] = MathUtil.randRange(0, _numTextureEffects);
			}
		}
	}
	
	protected void filterActiveTextures() {
		// if(_debugTextures == false) return; 
		for( int i=0; i < _activeTextures.size(); i++ ) {
			PGraphics pg = _activeTextures.get(i).texture();
			float filterTime = p.frameCount / 40f;
			
			if(_textureEffectsIndices[i] == 1) {
				KaleidoFilter.instance(p).setSides(4);
				KaleidoFilter.instance(p).setAngle(filterTime / 10f);
				KaleidoFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 2) {
				DeformTunnelFanFilter.instance(p).setTime(filterTime);
				DeformTunnelFanFilter.instance(p).applyTo(p);
			} else if(_textureEffectsIndices[i] == 3) {
				EdgesFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 4) {
				MirrorFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 5) {
				WobbleFilter.instance(p).setTime(filterTime);
				WobbleFilter.instance(p).setSpeed(0.5f);
				WobbleFilter.instance(p).setStrength(0.0004f);
				WobbleFilter.instance(p).setSize( 200f);
				WobbleFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 6) {
				InvertFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 7) {
				RadialRipplesFilter.instance(p).setTime(filterTime);
				RadialRipplesFilter.instance(p).setAmplitude(0.5f + 0.5f * P.sin(filterTime));
				RadialRipplesFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 8) {
				BadTVLinesFilter.instance(p).applyTo(pg);
//			} else if(_textureEffectsIndices[i] == 9) {
//				EdgesFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 10) {
				CubicLensDistortionFilter.instance(p).setTime(filterTime);
				CubicLensDistortionFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 11) {
				SphereDistortionFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 12) {
				HalftoneFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 13) {
				PixelateFilter.instance(p).setDivider(15f, 15f * pg.height/pg.width);
				PixelateFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 14) {
				DeformBloomFilter.instance(p).setTime(filterTime);
				DeformBloomFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 15) {
				DeformTunnelFanFilter.instance(p).setTime(filterTime);
				DeformTunnelFanFilter.instance(p).applyTo(pg);
			} else if(_textureEffectsIndices[i] == 16) {
				HueFilter.instance(p).setTime(filterTime);
				HueFilter.instance(p).applyTo(pg);
			}
//			WarperFilter.instance(p).setTime( _timeEaseInc / 5f);
//			WarperFilter.instance(p).applyTo(pg);
//			ColorDistortionFilter.instance(p).setTime( _timeEaseInc / 5f);
//			ColorDistortionFilter.instance(p).setAmplitude(1.5f + 1.5f * P.sin(radsComplete));
//			ColorDistortionFilter.instance(p).applyTo(pg);
//			OpenGLUtil.setTextureRepeat(_buffer);

		}
	}
	
	/////////////////////////////////////////////////////////////////
	// Global (PApplet-level) special effects
	/////////////////////////////////////////////////////////////////

	protected void postProcessFilters() {
		// brightness
		float brightMult = 2.8f;
		if(p.frameCount < 3) p.midi.controllerChange(3, 41, P.round(127f/brightMult));	// default to 1.0, essentially, with room to get up to 2.8f
		float brightnessVal = p.midi.midiCCPercent(3, 41) * brightMult;
		BrightnessFilter.instance(p).setBrightness(brightnessVal);
		BrightnessFilter.instance(p).applyTo(p);
		
//		SaturationFilter.instance(p).setSaturation(1.2f);
//		SaturationFilter.instance(p).applyTo(p);
		
		ContrastFilter.instance(p).setContrast(1.2f);
		ContrastFilter.instance(p).applyTo(p);

//		FXAAFilter.instance(p).applyTo(p);
		
//		ColorCorrectionFilter.instance(p).setBrightness(0.1f * P.sin(p.frameCount/10f));
//		ColorCorrectionFilter.instance(p).setContrast(1f + 0.1f * P.sin(p.frameCount/10f));
//		ColorCorrectionFilter.instance(p).setGamma(1f + 0.2f * P.cos(p.frameCount/10f));
//		ColorCorrectionFilter.instance(p).setBrightness(-0.1f);
//		ColorCorrectionFilter.instance(p).setContrast(1.2f);
//		ColorCorrectionFilter.instance(p).setGamma(1.4f);
//		ColorCorrectionFilter.instance(p).applyTo(p);

		
		// color distortion auto
		int distAutoFrame = p.frameCount % 6000;
		float distFrames = 100f;
		if(distAutoFrame <= distFrames) {
			float distAmpAuto = P.sin(distAutoFrame/distFrames * P.PI);
			p.midi.controllerChange(3, 42, P.round(127 * distAmpAuto));
			p.midi.controllerChange(3, 43, P.round(127 * distAmpAuto));
		}
		
		// color distortion
		float colorDistortionAmp = p.midi.midiCCPercent(3, 42) * 0.5f;
		float colorDistortionTimeMult = p.midi.midiCCPercent(3, 43);
		if(colorDistortionAmp > 0) {
			float prevTime = ColorDistortionFilter.instance(p).getTime();
			ColorDistortionFilter.instance(p).setTime(prevTime + 1/100f * colorDistortionTimeMult);
			ColorDistortionFilter.instance(p).setAmplitude(colorDistortionAmp);
			ColorDistortionFilter.instance(p).applyTo(p);
		}
	}
	
	protected void traverseTrigger() {
		getRandomGroup().traverseStart();
	}
	
	/////////////////////////////////////////////////////////////////
	// Beat detection & user override
	/////////////////////////////////////////////////////////////////
	
	protected void checkBeat() {
		if( audioIn.isBeat() == true && isBeatDetectMode() == true ) {
			updateTiming();
		}
	}
	
	protected boolean isBeatDetectMode() {
		return ( p.millis() - USER_INPUT_BEAT_TIMEOUT > _lastInputMillis );
	}
	
	public void resetBeatDetectMode() {
		_lastInputMillis = p.millis();
//		numBeatsDetected = 1;
	}
	
	/////////////////////////////////////////////////////////////////
	// User input
	/////////////////////////////////////////////////////////////////
	
	protected void handleInput( boolean isMidi ) {
		super.handleInput( isMidi );
		
//		if( p.key == 'a' || p.key == 'A' ){
//			_isAutoPilot = !_isAutoPilot;
//			P.println("_isAutoPilot = "+_isAutoPilot);
//		}
//		if( p.key == 'S' ){
//			_isStressTesting = !_isStressTesting;
//			P.println("_isStressTesting = "+_isStressTesting);
//		}
		if ( _colorTrigger.active() == true ) {
			resetBeatDetectMode();
			updateColor();
		}
		if ( _modeTrigger.active() == true ) {
			newMode();
			traverseTrigger();
		}
		if ( _lineModeTrigger.active() == true ) {
			resetBeatDetectMode();
			updateLineMode();
		}
		if ( _rotationTrigger.active() == true ) {
			resetBeatDetectMode();
			updateRotation();
		}
		if ( _timingTrigger.active() == true ) {
			resetBeatDetectMode();
			updateTiming();
		}
		if ( _timingSectionTrigger.active() == true ) {
			updateTimingSection();
		}
		if ( _newTextureTrigger.active() == true ) {
			cycleANewTexture(null);
		}
		if ( _bigChangeTrigger.active() == true ) {
			resetBeatDetectMode();
			bigChangeTrigger();
		}
		if ( _allSameTextureTrigger.active() == true ) {
			resetBeatDetectMode();
			setAllSameTexture();
		}
		if ( _audioInputUpTrigger.active() == true ) audioIn.gainUp();
		if ( _audioInputDownTrigger.active() == true ) audioIn.gainDown();
		if ( _brightnessUpTrigger.active() == true ) p.midi.controllerChange(3, 41, Math.round(127f * p.midi.midiCCPercent(3, 41) + 1));
		if ( _brightnessDownTrigger.active() == true ) p.midi.controllerChange(3, 41, Math.round(127f * p.midi.midiCCPercent(3, 41) - 1));
		if ( _debugTexturesTrigger.active() == true ) _debugTextures = !_debugTextures;
	}
	
	/////////////////////////////////////////////////////////////////
	// Group/polygon mode updates
	/////////////////////////////////////////////////////////////////

	protected void newMode() {
		for( int i=0; i < _mappingGroups.size(); i++ ) 
			_mappingGroups.get(i).newMode();
	}
	
	protected void updateColor() {
		// sometimes do all groups, but mostly pick a random group to change
		if( MathUtil.randRange(0, 100) > 80 ) {
			for( int i=0; i < _mappingGroups.size(); i++ )
				_mappingGroups.get(i).newColor();
		} else {
			getRandomGroup().newColor();
		}
	}
	
	protected void updateLineMode() {
		// sometimes do all groups, but mostly pick a random one to change
		if( MathUtil.randRange(0, 100) > 80 ) {
			for( int i=0; i < _mappingGroups.size(); i++ )
				_mappingGroups.get(i).newLineMode();
		} else {
			getRandomGroup().newLineMode();
		}
	}
	
	protected void updateRotation() {
		randomActiveTexture().newRotation();
		for( int i=0; i < _mappingGroups.size(); i++ )
			_mappingGroups.get(i).newRotation();
	}
	
	protected void updateTiming() {
		// pass beat detection on to textures and lighting
		if(_dmxLights != null) _dmxLights.updateDmxLightsOnBeat();
		if(_overlayTexturePool.size() > 0) _overlayTexturePool.get(0).updateTiming();
		for( int i=0; i < _activeTextures.size(); i++ ) _activeTextures.get(i).updateTiming();
		
		// make beat detection sequencing decisions
		numBeatsDetected++;
		
		if( numBeatsDetected % BEAT_INTERVAL_MAP_STYLE_CHANGE == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_MAP_STYLE_CHANGE");
			changeGroupsRandomPolygonMapStyle();
		}
		
		if( numBeatsDetected % BEAT_INTERVAL_COLOR == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_COLOR");
			updateColor();
		}
		if( numBeatsDetected % BEAT_INTERVAL_ROTATION == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_ROTATION");
			updateRotation();
		}
		if( numBeatsDetected % BEAT_INTERVAL_TRAVERSE == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_TRAVERSE");
			traverseTrigger();
		}
		if( numBeatsDetected % BEAT_INTERVAL_ALL_SAME == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_ALL_SAME");
			setGroupsMappingStylesToTheSame(true);
			setGroupsTextureToTheSameMaybe();
		}
		
		if( numBeatsDetected % BEAT_INTERVAL_LINE_MODE == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_LINE_MODE");
			updateLineMode();
		}
		
		if( numBeatsDetected % BEAT_INTERVAL_NEW_TIMING == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_NEW_TIMING");
			updateTimingSection();
		}
		
		if( numBeatsDetected % BEAT_INTERVAL_NEW_TEXTURE == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_NEW_TEXTURE");
			cycleANewTexture(null);
		}
		if( numBeatsDetected % BEAT_INTERVAL_BIG_CHANGE == 0 ) {
			if(_timingDebug == true) P.println("BEAT_INTERVAL_BIG_CHANGE");
			bigChangeTrigger();
		}
	}
	
	protected void updateTimingSection() {
		if(_overlayTexturePool.size() > 0) _overlayTexturePool.get(0).updateTimingSection();

		for( int i=0; i < _activeTextures.size(); i++ ) {
			_activeTextures.get(i).updateTimingSection();
		}
		
		newLineModeForRandomGroup();
		selectNewActiveTextureFilters();
	}
	
	protected void bigChangeTrigger() {
		// bail if the face recording texture is active
		if(_faceRecorder != null && _faceRecordingTexture != null) {
			if(_faceRecordingTexture.isActive() == true) return;
		}
		
		// randomize all textures to polygons & global effects
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).randomTextureToRandomPolygon();
		}
		selectNewActiveTextureFilters();
		
		// swap in a new overlay texture and normal pool texture
		cycleANewTexture(null);
		nextOverlayTexture();

		// do a couple of normal triggers
		newLineModesForAllGroups();
		updateTimingSection();
		
		// reset rotations
		for(int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).resetRotation();
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// cool rules (should just be across groups or forward timing/triggers to all groups)
	/////////////////////////////////////////////////////////////////

	protected void setGroupsTextureToTheSameMaybe() {
		// maybe also set a group to all to be the same texture
		for(int i=0; i < _mappingGroups.size(); i++ ) {
			if( MathUtil.randRange(0, 100) < 25 ) {
				_mappingGroups.get(i).setAllPolygonsToSameRandomTexture();
			}
		}
	}	
	
	protected void changeGroupsRandomPolygonMapStyle() {
		// every beat, change a polygon mapping style or texture
		for(int i=0; i < _mappingGroups.size(); i++ ) {
			if( MathUtil.randBoolean(p) == true ) {
				_mappingGroups.get(i).randomTextureToRandomPolygon();
			} else {
				_mappingGroups.get(i).randomPolygonRandomMappingStyle();
			}
		}
	}
	
	protected void setGroupsMappingStylesToTheSame(boolean allowsFullEQ) {
		// every once in a while, set all polygons' styles to be the same per group
		for(int i=0; i < _mappingGroups.size(); i++ ) {
			if( MathUtil.randRange(0, 100) < 90 || allowsFullEQ == false ) {
				_mappingGroups.get(i).setAllPolygonsTextureStyle( MathUtil.randRange(0, 2) );
			} else {
				_mappingGroups.get(i).setAllPolygonsTextureStyle( IMappedPolygon.MAP_STYLE_EQ );	// less likely to go to EQ fill
			}
			_mappingGroups.get(i).newColor();
		}
	}

	
	protected void newLineModeForRandomGroup() {
		getRandomGroup().newLineMode();
	}
	
	protected void newLineModesForAllGroups() {
		// set new line mode
		for(int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).newLineMode();
		}
		// once in a while, reset all mesh lines to the same random mode
		if( MathUtil.randRange(0, 100) < 10 ) {
			int newLineMode = MathUtil.randRange(0, MODE.values().length - 1);
			for(int i=0; i < _mappingGroups.size(); i++ ) {
				_mappingGroups.get(i).resetLineModeToIndex( newLineMode );
			}
		}
	}
		


	protected void setAllSameTexture() {
		boolean mode = MathUtil.randBoolean(p);
		BaseTexture newTexture = _texturePool.get(nextTexturePoolIndex());
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).clearAllTextures();
			_mappingGroups.get(i).pushTexture( newTexture );
			_mappingGroups.get(i).setAllPolygonsToTexture(0);
			if( mode == true ) {
				_mappingGroups.get(i).setAllPolygonsTextureStyle( IMappedPolygon.MAP_STYLE_CONTAIN_RANDOM_TEX_AREA );
			} else {
				_mappingGroups.get(i).setAllPolygonsTextureStyle( IMappedPolygon.MAP_STYLE_MASK );
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// Face recorded insanity
	/////////////////////////////////////////////////////////////////
	
	public void updateFaceRecorder() {
		_faceRecorder.update(_faceRecordingTexture.isActive(), _facesPlaybackTexture.isActive());
	}
	
	public void startFaceRecording() {
		setAllFaceRecorder();
		_faceRecordingTexture.setActive(true);
	}
	
	public void stopFaceRecording() {
		_faceRecordingTexture.setActive(false);
		removeFaceRecorderTexture();
		cycleANewTexture(_facesPlaybackTexture);
		bigChangeTrigger();
		updateTiming();
		updateTimingSection();
	}
	
	protected void setAllFaceRecorder() {
		// this is fucked because we're just adding to mapping groups without adding to the _curTexturePool. removal below is funky
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).clearAllTextures();
			_mappingGroups.get(i).pushTextureFront(_faceRecordingTexture);
			_mappingGroups.get(i).setAllPolygonsToTexture(0);
			_mappingGroups.get(i).setAllPolygonsTextureStyle( IMappedPolygon.MAP_STYLE_MASK );
		}
	}	

	protected void removeFaceRecorderTexture() {
		for( int i=0; i < _mappingGroups.size(); i++ ) {
			_mappingGroups.get(i).clearAllTextures();
		}

//		for( int i=0; i < _curTexturePool.size(); i++ ) {
//			P.println("remove attempt!@! ", _curTexturePool.get(i));
//			if( _curTexturePool.get(i) instanceof TextureKinectFaceRecording ) {
//				_curTexturePool.remove(i);
//				P.println("removed TextureKinectFaceRecording!!!");
//				return;
//			}
//		}
	}
	
}
