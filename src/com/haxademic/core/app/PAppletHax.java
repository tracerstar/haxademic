package com.haxademic.core.app;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import com.haxademic.core.app.config.AppSettings;
import com.haxademic.core.app.config.P5Properties;
import com.haxademic.core.audio.analysis.input.AudioInputBeads;
import com.haxademic.core.audio.analysis.input.AudioInputESS;
import com.haxademic.core.audio.analysis.input.AudioInputMinim;
import com.haxademic.core.audio.analysis.input.AudioStreamData;
import com.haxademic.core.audio.analysis.input.IAudioInput;
import com.haxademic.core.data.constants.PRenderers;
import com.haxademic.core.data.store.AppStore;
import com.haxademic.core.debug.DebugUtil;
import com.haxademic.core.debug.DebugView;
import com.haxademic.core.debug.Stats;
import com.haxademic.core.draw.context.DrawUtil;
import com.haxademic.core.draw.context.OpenGLUtil;
import com.haxademic.core.draw.image.MovieBuffer;
import com.haxademic.core.file.FileUtil;
import com.haxademic.core.hardware.browser.BrowserInputState;
import com.haxademic.core.hardware.gamepad.GamepadListener;
import com.haxademic.core.hardware.gamepad.GamepadState;
import com.haxademic.core.hardware.keyboard.KeyboardState;
import com.haxademic.core.hardware.kinect.IKinectWrapper;
import com.haxademic.core.hardware.kinect.KinectWrapperV1;
import com.haxademic.core.hardware.kinect.KinectWrapperV2;
import com.haxademic.core.hardware.kinect.KinectWrapperV2Mac;
import com.haxademic.core.hardware.midi.MidiDevice;
import com.haxademic.core.hardware.osc.OscWrapper;
import com.haxademic.core.hardware.webcam.WebCamWrapper;
import com.haxademic.core.math.easing.EasingFloat;
import com.haxademic.core.render.AnimationLoop;
import com.haxademic.core.render.GifRenderer;
import com.haxademic.core.render.ImageSequenceRenderer;
import com.haxademic.core.render.JoonsWrapper;
import com.haxademic.core.render.MIDISequenceRenderer;
import com.haxademic.core.render.VideoRenderer;
import com.haxademic.core.system.AppUtil;
import com.haxademic.core.system.JavaInfo;
import com.haxademic.core.system.SecondScreenViewer;
import com.haxademic.core.system.SystemUtil;
import com.haxademic.core.ui.UIButton;
import com.haxademic.core.ui.UIControlPanel;
import com.jogamp.newt.opengl.GLWindow;

import de.voidplus.leapmotion.LeapMotion;
import krister.Ess.AudioInput;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PSurface;
import processing.opengl.PJOGL;
import processing.video.Movie;
import themidibus.MidiBus;

public class PAppletHax
extends PApplet {
	//	Simplest launch:
	//	public static void main(String args[]) { PAppletHax.main(Thread.currentThread().getStackTrace()[1].getClassName()); }
	
	//	Fancier launch:
	//	public static void main(String args[]) {
	//		PAppletHax.main(P.concat(args, new String[] { "--hide-stop", "--bgcolor=000000", Thread.currentThread().getStackTrace()[1].getClassName() }));
	//		PApplet.main(new String[] { "--hide-stop", "--bgcolor=000000", "--location=1920,0", "--display=1", ElloMotion.class.getName() });
	//	}
		
	//	public static String arguments[];
	//	public static void main(String args[]) {
	//		arguments = args;
	//		PAppletHax.main(Thread.currentThread().getStackTrace()[1].getClassName());
	//	}

	// app
	protected static PAppletHax p;				// Global/static ref to PApplet - any class can access reference from this static ref. Easier access via `P.p`
	public PGraphics pg;						// Offscreen buffer that matches the app size
	public P5Properties appConfig;				// Loads the project .properties file to configure several app properties externally.
	protected String customPropsFile = null;	// Loads an app-specific project .properties file.
	protected String renderer; 					// The current rendering engine
	protected Robot _robot;
	public GLWindow window;
	protected boolean alwaysOnTop = false;

	// audio
	public IAudioInput audioInput;
	public AudioStreamData audioData = new AudioStreamData();
	public PGraphics audioInputDebugBuffer;

	// rendering
	public VideoRenderer videoRenderer;
	public ImageSequenceRenderer imageSequenceRenderer;
	public MIDISequenceRenderer midiRenderer;
	public GifRenderer gifRenderer;
	protected Boolean isRendering = true;
	protected Boolean renderingAudio = false;
	protected Boolean renderingMidi = true;
	public JoonsWrapper joons;
	public AnimationLoop loop = null;

	// input
	public WebCamWrapper webCamWrapper = null;
	public MidiDevice midiState = null;
	public MidiBus midiBus;
	public KeyboardState keyboardState;
	public IKinectWrapper kinectWrapper = null;
	public GamepadState gamepadState;
	public GamepadListener gamepadListener;
	public LeapMotion leapMotion = null;
	public OscWrapper oscState = null;
	public BrowserInputState browserInputState = null;
	public int lastMouseTime = 0;
	public boolean mouseShowing = true;
	public EasingFloat mouseXEase = new EasingFloat(0, 0.15f);
	public EasingFloat mouseYEase = new EasingFloat(0, 0.15f);

	// debug
	public int _fps;
	public Stats _stats;
	public DebugView debugView;
	public UIControlPanel ui;
	public SecondScreenViewer appViewerWindow;
	
	////////////////////////
	// INIT
	////////////////////////
	
	public void settings() {
		P.p = p = this;
		P.store = AppStore.instance();
		AppUtil.setFrameBackground(p,0,255,0);
		loadAppConfig();
		overridePropsFile();
		setAppIcon();
		setRenderer();
		setSmoothing();
		setRetinaScreen();
	}
	
	protected void loadAppConfig() {
		appConfig = new P5Properties(p);
		if( customPropsFile != null ) appConfig.loadPropertiesFile( customPropsFile );
		customPropsFile = null;
	}
	
	public void setAppIcon() {
		String appIconFile = p.appConfig.getString(AppSettings.APP_ICON, "haxademic/images/haxademic-logo.png");
		String iconPath = FileUtil.getFile(appIconFile);
		if(FileUtil.fileExists(iconPath)) {
			PJOGL.setIcon(iconPath);
		}
	}
	
	public void setup() {
		window = (GLWindow) surface.getNative();
		if(customPropsFile != null) DebugUtil.printErr("Make sure to load custom .properties files in settings()");
		setAppletProps();
		if(renderer != PRenderers.PDF) {
			debugView = new DebugView( p );
			debugView.active(p.appConfig.getBoolean(AppSettings.SHOW_DEBUG, false));
			addKeyCommandInfo();
			ui = new UIControlPanel();
			if(p.appConfig.getBoolean(AppSettings.SHOW_SLIDERS, false) == true) {
				ui.active(!ui.active());
			}
		}
		_stats = new Stats( p );
	}
	
	////////////////////////
	// INIT GRAPHICS
	////////////////////////
	
	protected void setRetinaScreen() {
		if(p.appConfig.getBoolean(AppSettings.RETINA, false) == true) {
			if(p.displayDensity() == 2) {
				p.pixelDensity(2);
			} else {
				DebugUtil.printErr("Error: Attempting to set retina drawing on a non-retina screen");
			}
		}	
	}
	
	protected void setSmoothing() {
		if(p.appConfig.getInt(AppSettings.SMOOTHING, AppSettings.SMOOTH_HIGH) == 0) {
			p.noSmooth();
		} else {
			p.smooth(p.appConfig.getInt(AppSettings.SMOOTHING, AppSettings.SMOOTH_HIGH));	
		}
	}
	
	protected void setRenderer() {
		PJOGL.profile = 4;
		renderer = p.appConfig.getString(AppSettings.RENDERER, P.P3D);
		if(p.appConfig.getBoolean(AppSettings.SPAN_SCREENS, false) == true) {
			// run fullscreen across all screens
			p.fullScreen(renderer, P.SPAN);
		} else if(p.appConfig.getBoolean(AppSettings.FULLSCREEN, false) == true) {
			// run fullscreen - default to screen #1 unless another is specified
			if(p.appConfig.getInt(AppSettings.FULLSCREEN_SCREEN_NUMBER, 1) != 1) DebugUtil.printErr("AppSettings.FULLSCREEN_SCREEN_NUMBER is busted if not screen #1. Use AppSettings.SCREEN_X, etc.");
			p.fullScreen(renderer); // , p.appConfig.getInt(AppSettings.FULLSCREEN_SCREEN_NUMBER, 1)
		} else if(p.appConfig.getBoolean(AppSettings.FILLS_SCREEN, false) == true) {
			// fills the screen, but not fullscreen
			p.size(displayWidth,displayHeight,renderer);
		} else {
			if(renderer == PRenderers.PDF) {
				// set headless pdf output file
				p.size(p.appConfig.getInt(AppSettings.WIDTH, 800),p.appConfig.getInt(AppSettings.HEIGHT, 600), renderer, p.appConfig.getString(AppSettings.PDF_RENDERER_OUTPUT_FILE, "output/output.pdf"));
			} else {
				// run normal P3D renderer
				p.size(p.appConfig.getInt(AppSettings.WIDTH, 800),p.appConfig.getInt(AppSettings.HEIGHT, 600), renderer);
			}
		}
	}
	
	protected void checkScreenManualPosition() {
		boolean isFullscreen = p.appConfig.getBoolean(AppSettings.FULLSCREEN, false);
		// check for additional screen_x params to manually place the window
		if(p.appConfig.getInt("screen_x", -1) != -1) {
			if(isFullscreen == false) {
				DebugUtil.printErr("Error: Manual screen positioning requires AppSettings.FULLSCREEN = true");
				return;
			}
			surface.setSize(p.appConfig.getInt(AppSettings.WIDTH, 800), p.appConfig.getInt(AppSettings.HEIGHT, 600));
			surface.setLocation(p.appConfig.getInt(AppSettings.SCREEN_X, 0), p.appConfig.getInt(AppSettings.SCREEN_Y, 0));  // location has to happen after size, to break it out of fullscreen
		}
	}

	////////////////////////
	// INIT OBJECTS
	////////////////////////
	
	protected void setAppletProps() {
		isRendering = p.appConfig.getBoolean(AppSettings.RENDERING_MOVIE, false);
		if( isRendering == true ) DebugUtil.printErr("When rendering, make sure to call super.keyPressed(); for esc key shutdown");
		renderingAudio = p.appConfig.getString(AppSettings.RENDER_AUDIO_FILE, "").length() > 0;
		renderingMidi = p.appConfig.getString(AppSettings.RENDER_MIDI_FILE, "").length() > 0;
		_fps = p.appConfig.getInt(AppSettings.FPS, 60);
		if(p.appConfig.getInt(AppSettings.FPS, 60) != 60) frameRate(_fps);
	}
	
	protected void addKeyCommandInfo() {
		p.debugView.setHelpLine(DebugView.TITLE_PREFIX + "KEY COMMANDS:", "");
		p.debugView.setHelpLine("ESC |", "Quit");
		p.debugView.setHelpLine("[F]", "Toggle `alwaysOnTop`");
		p.debugView.setHelpLine("[/]", "Toggle `DebugView`");
		p.debugView.setHelpLine("[\\]", "Toggle `PrefsSilders`");
		p.debugView.setHelpLine("[.]", "Audio input gain up");
		p.debugView.setHelpLine("[,]", "Audio input gain down");
		p.debugView.setHelpLine("[|]", "Save screenshot");
	}
	
	protected void initHaxademicObjects() {
		// create offscreen buffer
		pg = p.createGraphics(p.width, p.height, P.P3D);
		DrawUtil.setTextureRepeat(pg, true);
		// audio input
		initAudioInput();
		// animation loop
		if(p.appConfig.getFloat(AppSettings.LOOP_FRAMES, 0) != 0) loop = new AnimationLoop(p.appConfig.getFloat(AppSettings.LOOP_FRAMES, 0), p.appConfig.getInt(AppSettings.LOOP_TICKS, 4));
		// save single reference for other objects
		if( appConfig.getInt(AppSettings.WEBCAM_INDEX, -1) >= 0 ) webCamWrapper = new WebCamWrapper(appConfig.getInt(AppSettings.WEBCAM_INDEX, -1), appConfig.getBoolean(AppSettings.WEBCAM_THREADED, true));
		videoRenderer = new VideoRenderer( _fps, VideoRenderer.OUTPUT_TYPE_MOVIE, p.appConfig.getString( "render_output_dir", FileUtil.getHaxademicOutputPath() ) );
		if(appConfig.getBoolean(AppSettings.RENDERING_GIF, false) == true) {
			gifRenderer = new GifRenderer(appConfig.getInt(AppSettings.RENDERING_GIF_FRAMERATE, 45), appConfig.getInt(AppSettings.RENDERING_GIF_QUALITY, 15));
		}
		if(appConfig.getBoolean(AppSettings.RENDERING_IMAGE_SEQUENCE, false) == true) {
			imageSequenceRenderer = new ImageSequenceRenderer(p.g);
		}
		initKinect();
		if( p.appConfig.getInt(AppSettings.MIDI_DEVICE_IN_INDEX, -1) >= 0 ) {
			MidiBus.list(); // List all available Midi devices on STDOUT. This will show each device's index and name.
			midiBus = new MidiBus(
					this, 
					p.appConfig.getInt(AppSettings.MIDI_DEVICE_IN_INDEX, 0), 
					p.appConfig.getInt(AppSettings.MIDI_DEVICE_OUT_INDEX, 0)
					);
		}
		midiState = new MidiDevice();
		keyboardState = new KeyboardState();
		browserInputState = new BrowserInputState();
		gamepadState = new GamepadState();
		if( p.appConfig.getBoolean( AppSettings.GAMEPADS_ACTIVE, false ) == true ) gamepadListener = new GamepadListener();
		if( p.appConfig.getBoolean( "leap_active", false ) == true ) leapMotion = new LeapMotion(this);
		if( p.appConfig.getBoolean( AppSettings.OSC_ACTIVE, false ) == true ) oscState = new OscWrapper();
		joons = ( p.appConfig.getBoolean(AppSettings.SUNFLOW, false ) == true ) ?
				new JoonsWrapper( p, width, height, ( p.appConfig.getString(AppSettings.SUNFLOW_QUALITY, "low" ) == AppSettings.SUNFLOW_QUALITY_HIGH ) ? JoonsWrapper.QUALITY_HIGH : JoonsWrapper.QUALITY_LOW, ( p.appConfig.getBoolean(AppSettings.SUNFLOW_ACTIVE, true ) == true ) ? true : false )
				: null;
		try { _robot = new Robot(); } catch( Exception error ) { println("couldn't init Robot for screensaver disabling"); }
		if(p.appConfig.getBoolean(AppSettings.APP_VIEWER_WINDOW, false) == true) appViewerWindow = new SecondScreenViewer(p.g, p.appConfig.getFloat(AppSettings.APP_VIEWER_SCALE, 0.5f));
		// check for always on top
		boolean isFullscreen = p.appConfig.getBoolean(AppSettings.FULLSCREEN, false);
		if(isFullscreen == true) {
			alwaysOnTop = p.appConfig.getBoolean(AppSettings.ALWAYS_ON_TOP, true);
			if(alwaysOnTop) AppUtil.setAlwaysOnTop(p, true);
		}
	}
	
	protected void initKinect() {
		if( p.appConfig.getBoolean( AppSettings.KINECT_V2_WIN_ACTIVE, false ) == true ) {
			kinectWrapper = new KinectWrapperV2( p, p.appConfig.getBoolean( "kinect_depth", true ), p.appConfig.getBoolean( "kinect_rgb", true ), p.appConfig.getBoolean( "kinect_depth_image", true ) );
		} else if( p.appConfig.getBoolean( AppSettings.KINECT_V2_MAC_ACTIVE, false ) == true ) {
			kinectWrapper = new KinectWrapperV2Mac( p, p.appConfig.getBoolean( "kinect_depth", true ), p.appConfig.getBoolean( "kinect_rgb", true ), p.appConfig.getBoolean( "kinect_depth_image", true ) );
		} else if( p.appConfig.getBoolean( AppSettings.KINECT_ACTIVE, false ) == true ) {
			kinectWrapper = new KinectWrapperV1( p, p.appConfig.getBoolean( "kinect_rgb", true ), p.appConfig.getBoolean( "kinect_depth_image", true ) );
		}
		if(kinectWrapper != null) {
			kinectWrapper.setMirror( p.appConfig.getBoolean( "kinect_mirrored", true ) );
			kinectWrapper.setFlipped( p.appConfig.getBoolean( "kinect_flipped", false ) );
		}
	}
	
	protected void initAudioInput() {
		if(appConfig.getBoolean(AppSettings.AUDIO_DEBUG, false) == true) JavaInfo.printAudioInfo();
		if( appConfig.getBoolean(AppSettings.INIT_MINIM_AUDIO, false) == true ) {
			audioInput = new AudioInputMinim();
		} else if( appConfig.getBoolean(AppSettings.INIT_BEADS_AUDIO, false) == true ) {
			audioInput = new AudioInputBeads();
		} else if( appConfig.getBoolean(AppSettings.INIT_ESS_AUDIO, true) == true ) {
			// Default to ESS being on, unless a different audio library is selected
			try {
				audioInput = new AudioInputESS();
				DebugUtil.printErr("Fix AudioInputESS amp: audioStreamData.setAmp(fft.max);");
			} catch (IllegalArgumentException e) {
				DebugUtil.printErr("ESS Audio not initialized. Check your sound card settings.");
			}
		}
		// if we've initialized an audio input, let's build an audio buffer
		if(audioInput != null) {
			createAudioDebugBuffer();
		}
	}
	
	protected void createAudioDebugBuffer() {
		audioInputDebugBuffer = p.createGraphics((int) AudioStreamData.debugW, (int) AudioStreamData.debugW, PRenderers.P3D);
		debugView.setTexture(audioInputDebugBuffer);
	}

	// option to override 
	public void setAudioInput(IAudioInput input) {
		audioInput = input;
		// create audio buffer if an audio input never did
		if(audioInputDebugBuffer == null) {
			createAudioDebugBuffer();
		}
	}
	
	protected void initializeOn1stFrame() {
		if( p.frameCount == 1 ) {
			P.println("Using Java version: " + SystemUtil.getJavaVersion() + " and GL version: " + OpenGLUtil.getGlVersion(p.g));
			initHaxademicObjects();
			setupFirstFrame();
		}
		if(p.frameCount == 10) {
			// move screen after first frame is rendered. this prevents weird issues (i.e. the app not even starting)
			checkScreenManualPosition();
		}
	}
	
	////////////////////////
	// OVERRIDES
	////////////////////////

	protected void overridePropsFile() {
		if( customPropsFile == null ) P.println("YOU SHOULD OVERRIDE overridePropsFile(). Using run.properties");
	}

	protected void setupFirstFrame() {
		// YOU SHOULD OVERRIDE setupFirstFrame() to avoid 5000ms Processing/Java timeout in setup()
	}

	protected void drawApp() {
		P.println("YOU MUST OVERRIDE drawApp()");
	}
	
	////////////////////////
	// GETTERS
	////////////////////////

	// app surface
	
	public PSurface getSurface() {
		return surface;
	}
	
	public boolean alwaysOnTop() {
		return alwaysOnTop;
	}
	
	// audio
	
	public float[] audioFreqs() {
		return audioInput.audioData().frequencies();
	}
	
	public float audioFreq(int index) {
//		return audioFreqMod(index, audioFreqs().length);
		return audioFreqMod(index, 128);
	}
		
	public float audioFreqMod(int index, int mod) {
		return audioFreqs()[index % mod];
	}
	
	////////////////////////
	// DRAW
	////////////////////////
	
	public void draw() {
		initializeOn1stFrame();
		killScreensaver();
		if(loop != null) loop.update();
		updateAudioData();
		handleRenderingStepthrough();
		midiState.update();
		updateEasedMouse();
		if( kinectWrapper != null ) kinectWrapper.update();
		p.pushMatrix();
		if( joons != null ) joons.startFrame();
		drawApp();
		if( joons != null ) joons.endFrame( p.appConfig.getBoolean(AppSettings.SUNFLOW_SAVE_IMAGES, false) == true );
		p.popMatrix();
		renderFrame();
		keyboardState.update();
		gamepadState.update();
		browserInputState.update();
		autoHideMouse();
		if(oscState != null) oscState.update();
		showStats();
		keepOnTop();
		setAppDockIconAndTitle(false);
		if(renderer == PRenderers.PDF) finishPdfRender();
	}
	
	////////////////////////
	// UPDATE OBJECTS
	////////////////////////	

	protected void updateEasedMouse() {
		mouseXEase.setTarget(mousePercentX());
		mouseXEase.update();
		mouseYEase.setTarget(mousePercentY());
		mouseYEase.update();
	}
	
	protected void updateAudioData() {
		if(audioInput != null) {
			PGraphics audioBuffer = (debugView.active() == true) ? audioInputDebugBuffer : null;	// only draw if debugging
			if(audioBuffer != null) {
				audioBuffer.beginDraw();
				audioBuffer.background(0);
			}
			audioInput.update(audioBuffer);
			audioData = audioInput.audioData();
			if(audioBuffer != null) audioBuffer.endDraw();
		}
	}

	protected void showStats() {
		p.noLights();
		_stats.update();
		debugView.draw();
		ui.update();
	}

	protected void keepOnTop() {
		if(alwaysOnTop == true) {
			if(p.frameCount % 600 == 0) AppUtil.requestForegroundSafe();
		}
	}
	
	protected void setAppDockIconAndTitle(boolean showFPS) {
		if(renderer != PRenderers.PDF) {
			if(p.frameCount == 1) {
				AppUtil.setTitle(p, p.appConfig.getString(AppSettings.APP_NAME, "Haxademic | " + this.getClass().getSimpleName()));
//				AppUtil.setAppToDockIcon(p);
			} else if(appConfig.getBoolean(AppSettings.SHOW_FPS_IN_TITLE, false)) {
				AppUtil.setTitle(p, p.appConfig.getString(AppSettings.APP_NAME, "Haxademic | " + this.getClass().getSimpleName()) + " | " + P.round(p.frameRate) + "fps");
			}
		}	
	}
	
	////////////////////////
	// RENDERING
	////////////////////////
	
	protected void finishPdfRender() {
		P.println("Finished PDF render.");
		p.exit();
	}
	
	protected void handleRenderingStepthrough() {
		// step through midi file if set
		if( renderingMidi == true ) {
			if( p.frameCount == 1 ) {
				try {
					midiRenderer = new MIDISequenceRenderer(p);
					midiRenderer.loadMIDIFile( p.appConfig.getString(AppSettings.RENDER_MIDI_FILE, ""), p.appConfig.getFloat(AppSettings.RENDER_MIDI_BPM, 150f), _fps, p.appConfig.getFloat(AppSettings.RENDER_MIDI_OFFSET, -8f) );
				} catch (InvalidMidiDataException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
			}
		}
		// analyze & init audio if stepping through a render
		if( isRendering == true ) {
			if( p.frameCount == 1 ) {
				if( renderingAudio == true ) {
					videoRenderer.startRendererForAudio( p.appConfig.getString(AppSettings.RENDER_AUDIO_FILE, "") );
				} else {
					videoRenderer.startVideoRenderer();
				}
			}

			// have renderer step through audio, then special call to update the single WaveformData storage object
			if( renderingAudio == true ) {
				videoRenderer.analyzeAudio();
			}

			if( midiRenderer != null ) {
				boolean doneCheckingForMidi = false;
				while( doneCheckingForMidi == false ) {
					int rendererNote = midiRenderer.checkForCurrentFrameNoteEvents();
					if( rendererNote != -1 ) {
						midiState.noteOn( 0, rendererNote, 100 );
					} else {
						doneCheckingForMidi = true;
					}
				}
			}
		}
		if(gifRenderer != null && appConfig.getBoolean(AppSettings.RENDERING_GIF, false) == true) {
			if(appConfig.getInt(AppSettings.RENDERING_GIF_START_FRAME, 1) == p.frameCount) {
				gifRenderer.startGifRender(this);
			}
		}
		if(imageSequenceRenderer != null && appConfig.getBoolean(AppSettings.RENDERING_IMAGE_SEQUENCE, false) == true) {
			if(appConfig.getInt(AppSettings.RENDERING_IMAGE_SEQUENCE_START_FRAME, 1) == p.frameCount) {
				imageSequenceRenderer.startImageSequenceRender();
			}
		}
	}
	
	protected void renderFrame() {
		// gives the app 1 frame to shutdown after the movie rendering stops
		if( isRendering == true ) {
			if(p.frameCount >= appConfig.getInt(AppSettings.RENDERING_MOVIE_START_FRAME, 1)) {
				videoRenderer.renderFrame();
			}
			// check for movie rendering stop frame
			if(p.frameCount == appConfig.getInt(AppSettings.RENDERING_MOVIE_STOP_FRAME, 5000)) {
				videoRenderer.stop();
				P.println("shutting down renderer");
			}
		}
		// check for gif rendering stop frame
		if(gifRenderer != null && appConfig.getBoolean(AppSettings.RENDERING_GIF, false) == true) {
			if(appConfig.getInt(AppSettings.RENDERING_GIF_START_FRAME, 1) == p.frameCount) {
				gifRenderer.startGifRender(this);
			}
			DrawUtil.setColorForPImage(p);
			gifRenderer.renderGifFrame(p.g);
			if(appConfig.getInt(AppSettings.RENDERING_GIF_STOP_FRAME, 100) == p.frameCount) {
				gifRenderer.finish();
			}
		}
		// check for image sequence stop frame
		if(imageSequenceRenderer != null && appConfig.getBoolean(AppSettings.RENDERING_IMAGE_SEQUENCE, false) == true) {
			if(p.frameCount >= appConfig.getInt(AppSettings.RENDERING_IMAGE_SEQUENCE_START_FRAME, 1)) {
				imageSequenceRenderer.renderImageFrame();
			}
			if(p.frameCount == appConfig.getInt(AppSettings.RENDERING_IMAGE_SEQUENCE_STOP_FRAME, 500)) {
				imageSequenceRenderer.finish();
			}
		}
	}
	
	public void saveScreenshot(PGraphics savePG) {
		savePG.save(FileUtil.getHaxademicOutputPath() + "_screenshots/" + SystemUtil.getTimestamp(p) + ".png");
	}
	
	////////////////////////
	// INPUT
	////////////////////////
	
	protected void autoHideMouse() {
		// show mouse
		if(p.mouseX != p.pmouseX || p.mouseY != p.pmouseY) {
			lastMouseTime = p.millis();
			if(p.mouseShowing == false) {
				p.mouseShowing = true;
				p.cursor();
			}
		}
		// hide mouse
		if(p.mouseShowing == true) {
			if(p.millis() > lastMouseTime + 5000) {
				p.noCursor();
				mouseShowing = false;
			}
		}
	}
	
	protected void killScreensaver() {
		// keep screensaver off - hit shift every 1000 frames
		if( p.frameCount % 1000 == 10 ) _robot.keyPress(KeyEvent.VK_SHIFT);
		if( p.frameCount % 1000 == 11 ) _robot.keyRelease(KeyEvent.VK_SHIFT);
	}

	public void keyPressed() {
		// disable esc key - subclass must call super.keyPressed()
		if( p.key == P.ESC && ( p.appConfig.getBoolean(AppSettings.DISABLE_ESC_KEY, false) == true ) ) {   //  || p.appConfig.getBoolean(AppSettings.RENDERING_MOVIE, false) == true )
			key = 0;
//			renderShutdownBeforeExit();
		}
		keyboardState.setKeyOn(p.keyCode);
		
		// special core app key commands
		if (p.key == 'F') {
			alwaysOnTop = !alwaysOnTop;
			AppUtil.setAlwaysOnTop(p, alwaysOnTop);
		}
		
		// audio input gain
		if ( p.key == '.' ) {
			p.audioData.setGain(p.audioData.gain() + 0.05f);
			p.debugView.setValue("audioData.gain()", p.audioData.gain());
		}
		if ( p.key == ',' ) {
			p.audioData.setGain(p.audioData.gain() - 0.05f);
			p.debugView.setValue("audioData.gain()", p.audioData.gain());
		}
		// show debug & prefs sliders
		if (p.key == '|') saveScreenshot(p.g);
		if (p.key == '/') debugView.active(!debugView.active());
		if (p.key == '\\') ui.active(!ui.active());
	}
	
	public void keyReleased() {
		keyboardState.setKeyOff(p.keyCode);
	}
	
	public float mousePercentX() {
		return P.map(p.mouseX, 0, p.width, 0, 1);
	}

	public float mousePercentY() {
		return P.map(p.mouseY, 0, p.height, 0, 1);
	}
	
	public float mousePercentXEased() {
		return p.mouseXEase.value();
	}
	
	public float mousePercentYEased() {
		return p.mouseYEase.value();
	}
	
	////////////////////////
	// SHUTDOWN
	////////////////////////
	
	public void stop() {
		if(p.webCamWrapper != null) p.webCamWrapper.dispose();
		if( kinectWrapper != null ) {
			kinectWrapper.stop();
			kinectWrapper = null;
		}
		if( leapMotion != null ) leapMotion.dispose();
		super.stop();
	}

	////////////////////////
	// PAPPLET LISTENERS
	////////////////////////
	
	// Movie playback
	public void movieEvent(Movie m) {
		m.read();
		MovieBuffer.moviesEventFrames.put(m, p.frameCount);
	}

	// ESS audio input
	public void audioInputData(AudioInput theInput) {
		if(audioInput instanceof AudioInputESS) {
			((AudioInputESS) audioInput).audioInputCallback(theInput);
		}
	}

	// LEAP MOTION EVENTS
	void leapOnInit(){
	    // println("Leap Motion Init");
	}
	void leapOnConnect(){
	    // println("Leap Motion Connect");
	}
	void leapOnFrame(){
	    // println("Leap Motion Frame");
	}
	void leapOnDisconnect(){
	    // println("Leap Motion Disconnect");
	}
	void leapOnExit(){
	    // println("Leap Motion Exit");
	}

	public void uiButtonClicked(UIButton button) {
		P.out("uiButtonClicked: please override", button.id(), button.value());
	}

}
