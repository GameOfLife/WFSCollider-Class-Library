
(
~path = "sounds/a11wlk01-44_1.aiff";
x = BufSndFile(~path, loop: true);
z = UChain( x.makeUnit, \stereo_output );
g = z.gui;
)


( // gui with styling
w = Window( "x", Rect( 300,25,250,300 ) ).front;
RoundView.useWithSkin( ( 
	labelWidth: 50, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.33)
), { z.gui(w); };
);
)


// design for a BufSndFile plot window

(
		w = Window("soundfile test", Rect(200, 200, 850, 400), scroll: false);
		a = SCSoundFileView.new(w, w.view.bounds);
		a.resize_(5);
		f = x.asSoundFile;
		a.soundfile = f;
		a.read(0, f.numFrames);
		a.elasticMode_(1);
		a.gridOn = false;
		a.waveColors = Color.gray(0.2)!16;
		w.front;
		a.background = Color.clear;
		b = SmoothRangeSlider( w, a.bounds.insetAll(1,1,1,1) )
			.knobSize_(0)
			.resize_(5)
			.background_( nil )
			.hiliteColor_( Color.gray(0.2).alpha_(0.25) );
		b.action = { |sl|
			x.startFrame = (sl.lo * x.numFrames).round(1);
			x.endFrame = (sl.hi * x.numFrames).round(1);
		};
		b.lo = x.startFrame / x.numFrames;
		b.hi = x.endFrame / x.numFrames;
)

