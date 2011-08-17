(
c = WFSSpeakerConf.rect( 48, 48, 5, 5 );
m = \lines;

w = ScaledUserView.window( "WFSSpeakerConf", 
	Rect( 128, 64, 400, 400 ), 
	Rect.aboutPoint( 0@0, 20, 20 );
);
w.keepRatio = true;
w.drawFunc = { c.draw(m) };
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