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
	c.draw(m); 
	Pen.addArc( p * (1 @ -1), 0.164/2, 0, 2pi ).fill; 
	c.arrayConfs.collect(_.cornerPoints).do({ |pts|
		pts.do({ |p|
			Pen.addArc( p * (1 @ -1), 0.164/2, 0, 2pi ).fill; 
		});
	});
};
w.scale = 2;
);

(

w.mouseDownAction = { |v, x,y| p = (x @ y.neg);
	WFSCrossfader( p, c.arrayConfs ).kr.round(0.01).postln };
w.mouseMoveAction = { |v, x,y| p = (x @ y.neg); 
	WFSCrossfader( p, c.arrayConfs ).kr.round(0.01).postln };
)

c = WFSSpeakerConf.rect( 40, 56, 6, 4 ); w.refresh; // rectangular setup
c = WFSSpeakerConf.rect( 48, 48, 5, 5 ); w.refresh;
m = \points; w.refresh; // change plot mode
m = \lines; w.refresh;

c[0].offset = 1; w.refresh; // move first array to left
c[0].offset = 0; w.refresh; 

c[0].cornerAngles[1]/pi;

c[1].angle = 0.05pi; c.init; w.refresh; // change angle of second array
c[3].angle = 0.95pi; c.init; w.refresh;
c[1].angle = 0; w.refresh; 