WFSArrayPanSynth.generateAllDefs( { |defs| d = defs } ); // takes apx 30s, watch post window

d; // these are all the defs

// result: 104 files with a total size of 2.7MB
SynthDef.synthDefDir.openInFinder;

( // loading goes quite fast
{
s.loadDirectory( SynthDef.synthDefDir );
s.sync;
"done".postln;
}.fork;
)

// higher numbers:
WFSArrayPanSynth.maxSize = 96; // default is 64
WFSArrayPanSynth.generateAllDefs( { |defs| d = defs }, 60 ); // takes apx 60s

// typical uses: (check cpu count)

( // 2 arrays double (focused and normal), cubic int (hi-q)
x = [
	Synth( "wfsa_fdc_48" ), 
	Synth( "wfsa_fdc_48" ), 
	Synth( "wfsa_ndc_48" ),
	Synth( "wfsa_ndc_48" ) 
];
)

( // 2 arrays double (focused and normal), linear int
x = [
	Synth( "wfsa_fdl_40" ), 
	Synth( "wfsa_fdl_56" ), 
	Synth( "wfsa_ndl_40" ),
	Synth( "wfsa_ndl_56" ) 
];
)

( // 2 arrays plane, cubic int (hi-q)
x = [ 
	Synth( "wfsa_pdc_48" ),
	Synth( "wfsa_pdc_48" ) 
];
)

( // 2 arrays static (highest possible for one server)
x = [ 
	Synth( "wfsa_usn_48" ),
	Synth( "wfsa_usn_48" )
];
)
