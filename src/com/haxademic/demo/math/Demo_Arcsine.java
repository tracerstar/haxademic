package com.haxademic.demo.math;

import com.haxademic.core.app.P;
import com.haxademic.core.app.PAppletHax;
import com.haxademic.core.draw.context.DrawUtil;

public class Demo_Arcsine
extends PAppletHax {
	public static void main(String args[]) { PAppletHax.main(Thread.currentThread().getStackTrace()[1].getClassName()); }

	public void drawApp() {
		p.background(0);
		p.noStroke();
		DrawUtil.setDrawCenter(p);
		DrawUtil.setCenterScreen(p);

		float segments = 36;
		float radius = p.height * 0.4f;
		float segmentRads = P.TWO_PI / segments;
		for (float i = 0; i < segments; i++) {
			// plot circle
			float curRads = i * segmentRads;
			float x = P.cos(curRads);
			float y = P.sin(curRads);
			p.fill(255);
			p.ellipse(x * radius, y * radius, 6, 6);
			
			// plot arc sine & arc cosine
			p.fill(255, 0, 0);
			p.ellipse(P.acos(x) * radius, P.asin(y) * radius, 10, 10);
		}
	
		// show radius for y using arc sine
		float yMouse = (-1 + 2f * p.mousePercentY());
		float xMouse = P.cos(P.asin(yMouse));
		p.fill(0, 255, 0);
		p.ellipse(xMouse * radius, yMouse * radius, 15, 15);
		
		// plot even distribution of vertical circle slices
		float vertSpacings = 10f;
		for (int i = 0; i <= vertSpacings; i++) {
			float y = (-1 + 2f * (1f/vertSpacings) * i);
			float x = P.cos(P.asin(y));
			p.stroke(0, 0, 255);
			p.line(x * radius, y * radius, x * -radius, y * radius);
		}
	}

}

