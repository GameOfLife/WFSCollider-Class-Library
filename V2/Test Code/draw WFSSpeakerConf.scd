(
c = WFSSpeakerConf.rect( 48, 48, 5, 5 );
// c = WFSSpeakerConf.polygon( 6, 5, 192 );
m = \lines;
p = 0@10;
t = 5/344;
w = ScaledUserView.window( "WFSSpeakerConf", 
	Rect( 128, 64, 400, 400 ), 
	Rect.aboutPoint( 0@0, 20, 20 );
);
w.keepRatio = true;

w.mouseDownAction = { |vw, x, y|
	p = x @ y.neg;
};
w.mouseMoveAction = { |vw, x, y|
	p = x @ y.neg;
};

(
w.drawFunc = { 
	var speakerLines, speakerPoints, prePan, crossfader, crossfades, arrayPan;
	c.draw(m);
	Pen.use({ 
		Pen.scale( 1, -1 );
		Pen.addArc( p, 0.164, 0, 2pi ).fill;
		
		Pen.color = Color.red(0.5).alpha_(0.25); 
		Pen.addArc( p, t*344, 0, 2pi ).fill;
		
		speakerLines = c.arrayConfs;
		speakerPoints = speakerLines.collect(_.asPoints).collect(_.reverse);
		prePan = WFSPrePan().ar( 1, p )[0];
		crossfader = WFSCrossfader( p, speakerLines );
		
		crossfades = (crossfader.cornerfades * crossfader.arraysShouldRun( false ))
			.max( crossfader.arraysShouldRun( true ) );
	
		arrayPan = speakerLines.collect({ |line|
			WFSArrayPan( *line.asArray ).ar( 1, p )[0] - 0.06 + prePan;
		});
		
		arrayPan.collect({ |times, i|
			Pen.color = Color.blue(0.5).alpha_(crossfades[i]);
			speakerPoints[i].do({ |sp, ii|
				Pen.addArc( sp,
					/* ((t - times[ii])*344).max(0), */
					((t - times[ii] )*344).max(0),
					speakerLines[i].angle + 0.5pi, pi ).stroke;
			});
		});
		
	});
};
);

w.scale = 2;
);



c = WFSSpeakerConf.rect( 40, 56, 6, 4 ); w.refresh; // rectangular setup
c = WFSSpeakerConf.rect( 48, 48, 5, 5 ); w.refresh;
m = \points; w.refresh; // change plot mode
m = \lines; w.refresh;

c[0].offset = 1; w.refresh; // move first array to left
c[0].offset = 0; w.refresh; 

c[1].angle = 0.05pi; w.refresh; // change angle of second array
c[1].angle = 0; w.refresh; 