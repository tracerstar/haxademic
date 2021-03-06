package com.haxademic.core.draw.image;

import org.gstreamer.elements.PlayBin2;

import processing.video.Movie;

public class MovieFinishedListener {

	protected Movie movie;
	protected IMovieFinishedDelegate delegate;
	protected boolean connected = false;
	
	public MovieFinishedListener(Movie movie, IMovieFinishedDelegate delegate) {
		this.movie = movie;
		this.delegate = delegate;
		connect();
	}
	
	public void setMovie(Movie newMovie) {
		disconnect();
		movie = newMovie;
		connect();
	}
	
	public void connect() {
		if(connected) return;
		movie.playbin.connect(FinishCallback);
		connected = true;
	}
	
	public void disconnect() {
		if(!connected) return;
		movie.playbin.disconnect(FinishCallback);
		connected = false;
	}
	
	public void dispose() {
		disconnect();
		movie = null;
	}
	
	// GSTREAMER CALLBACK
	
	protected PlayBin2.ABOUT_TO_FINISH FinishCallback = new PlayBin2.ABOUT_TO_FINISH() {
		@Override
		public void aboutToFinish(PlayBin2 playbin) {
			delegate.videoFinished(movie);
		}
	};
	
	// PUBLIC CALLBACK INTERFACE
		
	public interface IMovieFinishedDelegate {
		public void videoFinished(Movie movie);
	}
}
