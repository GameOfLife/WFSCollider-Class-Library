(
c = WFSSpeakerConf.rect( 48, 48, 5, 5 );
m = \lines;
a = \point;
p = 0@7.5;
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
	var corners, polar, p1, p2;
	c.draw(m);
	corners = c.arrayConfs.collect(_.cornerPoints).flop[0];
	
	Pen.use({ 
		Pen.scale( 1, -1 );
		Pen.addArc( p, 0.164, 0, 2pi ).fill;
		
		
		Pen.line( 0.2@0, -0.2@0 ).line( 0 @ 0.2, 0 @ -0.2 ).stroke;
		
		Pen.color = Color.red(0.5).alpha_(0.25); 
		if( a === \point ) {
			Pen.addArc( p, t.max(0)*344, 0, 2pi ).stroke;
		} {
			polar = p.asPolar;
			polar.rho_( polar.rho - (t*344) );
			p1 = polar.asPoint;
			p2 = Polar( 10, polar.angle-0.5pi).asPoint;
			Pen.line( p1 + p2, p1 - p2 ).stroke;
		};
		
		
		Pen.moveTo( corners.last );
		corners.do({ |pt, i|
			Pen.lineTo( pt );
		});
		Pen.clip;
		
		
		speakerLines = c.arrayConfs;
		speakerPoints = speakerLines.collect(_.asPoints).collect(_.reverse);
		prePan = WFSPrePan().ar( 1, p )[0];
		
		if( a === \point ) {
			crossfader = WFSCrossfader( p, speakerLines );
			crossfades = (crossfader.cornerfades * crossfader.arraysShouldRun( false ))
				.max( crossfader.arraysShouldRun( true ) );
			arrayPan = speakerLines.collect({ |line|
				WFSArrayPan( *line.asArray ).focus_(nil).ar( 1, p ) + [-0.06 + prePan,0];
			});
		} {	
			crossfades = WFSCrossfaderPlane( p, speakerLines ).crossfades;
			arrayPan = speakerLines.collect({ |line|
				WFSArrayPanPlane( *line.asArray ).ar( 1, p ) + [-0.06 + prePan,0];
			});
		};
		
		arrayPan.collect({ |times, i|
			var color;
			color = Color.blue(0.5).alpha_(crossfades[i]);
			times = times.flop.flop;
			speakerPoints[i].do({ |sp, ii|
				Pen.color = color.copy.alpha_( (color.alpha * times[1][ii]).sqrt);
				Pen.addArc( sp, ((t - times[0][ii] )*344).max(0), 0, 2pi ).stroke;
			});
		});
		
	});
};
);

w.scale = 2;
);



c = WFSSpeakerConf.rect( 40, 56, 6, 4 ); w.refresh; // rectangular setup
c = WFSSpeakerConf.rect( 48, 48, 5, 5 ); w.refresh;
c = WFSSpeakerConf.polygon( 6, 5, 192 ); w.refresh;
m = \points; w.refresh; // change plot mode
m = \lines; w.refresh;

a = \point; w.refresh; // show point
a = \plane; w.refresh; // show plane

t = -5/344; w.refresh;
t = 0/344; w.refresh;
t = 5/344; w.refresh;
t = 10/344; w.refresh;

c[0].offset = 1; w.refresh; // move first array to left
c[0].offset = 0; w.refresh; 

c[1].angle = 0.05pi; w.refresh; // change angle of second array
c[1].angle = 0; w.refresh; 