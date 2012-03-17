WFSSpeakerConf.rect(48,48,5,5).makeDefault;
WFSSpeakerConf.addServer( s, 0 ); // for local testing

( // do this only if the defs aren't ever created yet. They will be stored in the synthdefs folder
WFSPrePanSynthDefs.generateAll;
WFSArrayPanSynthDefs.generateAll( { |defs| d = defs } ); // takes apx 30s, watch post window
)

( // loading goes quite fast
{
s.loadDirectory( SynthDef.synthDefDir );
s.sync;
"done".postln;
}.fork;
)

x = UChain( \sine, \wfsDynamicPoint );

x = UChain( \sine, \wfsStaticPoint );

x = UChain( \sine, \wfsStaticPlane );

x = UChain( \sine, \wfsDynamicPlane );


x.gui;

// or: 
(
~path = "sounds/a11wlk01-44_1.aiff";
z = BufSndFile("sounds/a11wlk01-44_1.aiff", loop: true);
x = UChain( z.makeUnit, \wfsDynamicPoint );
)



( // gui with styling
w = Window( "UChain", Rect( 300,25,340,440 ) ).front;
RoundView.useWithSkin( ( 
	labelWidth: 80, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.33)
), { x.gui(w); };
);
)

x[1].quality = \better; // set args from code
x[1].point = 0.5@0.6;
