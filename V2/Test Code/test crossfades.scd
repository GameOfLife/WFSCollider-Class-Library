(
c = WFSSpeakerConf.rect( 48, 48, 5, 5 );
m = \lines;
p = (0@0);
w = ScaledUserView.window( "WFSSpeakerConf", 
	Rect( 128, 64, 400, 400 ), 
	Rect.aboutPoint( 0@0, 20, 20 );
);
w.keepRatio = true;
w.drawFunc = { 
	var fader, crossfades, lines;
	c.draw(m); 
	Pen.addArc( p * (1 @ -1), 0.3/2, 0, 2pi ).fill; 
	
	Pen.color = Color.blue;
	c.arrayConfs.collect(_.cornerPoints).do({ |pts| // draw corner points
		pts.do({ |p|
			Pen.addArc( p * (1 @ -1), 0.164/2, 0, 2pi ).fill; 
		});
	});
	
	// draw normal as blue lines behind arrays
	fader = WFSCrossfader( p, c.arrayConfs );
	crossfades = fader.cornerfades;
	lines = c.asLines;
	Pen.use({
		Pen.scale(1.1, -1.1);
		Pen.width = 0.2;
		fader.arraysShouldRun( false ).do({ |fade, i|
			if( fade == 1 ) {
				Pen.color = Color.blue.alpha_( crossfades[i] );
				Pen.line( *lines[i] ).stroke;
			};
		});
	});
	
	Pen.use({
		Pen.scale(0.9, -0.9);
		Pen.color = Color.blue;
		Pen.width = 0.2;
		fader.arraysShouldRun( true ).do({ |fade, i|
			if( fade == 1 ) {
				Pen.line( *lines[i] ).stroke;
			};
		});
	});

};
w.scale = 2;
w.mouseDownAction = { |v, x,y| p = (x @ y.neg); };
w.mouseMoveAction = { |v, x,y| p = (x @ y.neg); };
);


c = WFSSpeakerConf.rect( 40, 56, 6, 4 ); w.refresh; // rectangular setup
c = WFSSpeakerConf.rect( 48, 48, 5, 5 ); w.refresh;
m = \points; w.refresh; // change plot mode
m = \lines; w.refresh;

c[0].offset = 1; w.refresh; // move first array to left
c[0].offset = 0; w.refresh; 

c[0].cornerAngles[1]/pi;

c[1].angle = 0.025pi; c.init; w.refresh; // change angle of second array
c[2].angle = -0.49pi; c.init; w.refresh; // change angle of second array
c[3].angle = -1.025pi; c.init; w.refresh;