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
	var fades, lines;
	c.draw(m); 
	Pen.addArc( p * (1 @ -1), 0.3/2, 0, 2pi ).fill; 
	
	Pen.color = Color.blue;
	c.arrayConfs.collect(_.cornerPoints).do({ |pts| // draw corner points
		pts.do({ |p|
			Pen.addArc( p * (1 @ -1), 0.164/2, 0, 2pi ).fill; 
		});
	});
	
	// draw normal as blue lines behind arrays
	fades = WFSCrossfader( p, c.arrayConfs ).kr;
	lines = c.asLines;
	Pen.use({
		Pen.scale(1.1, -1.1);
		Pen.width = 0.2;
		fades.do({ |fade, i|
			if( (fade[0] > 0) && { fade[1] != 1 }) {
				Pen.color = Color.blue.alpha_( fade[0] );
				Pen.line( *lines[i] ).stroke;
			};
		});
	});
	
	Pen.use({
		Pen.scale(0.9, -0.9);
		Pen.color = Color.blue;
		Pen.width = 0.2;
		fades.do({ |fade, i|
			if( fade[1] > 0 && { fade[2] > 0 }) {
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