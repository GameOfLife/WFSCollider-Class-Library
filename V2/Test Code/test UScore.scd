(
z = UScore(
	*12.collect({ |i|
		var evt;
		evt = UEvent(
			BufSndFile( "sounds/a11wlk01-44_1.aiff", 
				rate: (i-6).midiratio, loop: [true,false].wrapAt(i) ).makeUChain,
			i/2
		);
		if( evt.object.duration == inf ) {
			evt.eventDuration = 8; // looped events stopped by UScore
		};
		evt;
	})
);
)


z.prepare( s, 0, { "done".postln } );
z.start( s, 0, true );

z.pause; // looped events keep playing
z.resume;

z.stop;

z.start( s, 0, false ); // takes extra wait time for preparing events (depends on content)

z.pos; // returns current position (keeps increasing if not stopped