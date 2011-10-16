/*
UEnv automatically creates a standard envelope for a U (unit).

It creates the following private controls automatically:

	u_gate (1) : a gate to release the envelope
	u_dur (inf) : a duration (s) for the unit, may be inf (default)
	u_doneAction (0) : a doneAction for when the envelope is released or finished
	                   this may release the unit itself (2) or the whole UChain (14)
	u_gain (0) : overall gain in dB (0 == no change, -inf = silent)
	u_fadeIn (0) : fade in time (s)
	u_fadeOut (0) : fade out time (s), within the duration or after the release

An UEnv is typically applied once in a unit that has output(s) to the hardware buses.
In UChains it is used internally for releasing, gain and setting the overal level.
Units containing a UEnv are usually found at the tail of a UChain, and UChains should
contain at least one unit with a UEnv (otherwise they have no control over fade times, 
duration etc.).
 
*/

UEnv : UIn {
	
	*initClass {
		Class.initClassTree(ControlSpec);
		ControlSpec.specs = ControlSpec.specs.addAll([

			\u_dur -> ControlSpec(0, inf),
			\u_doneAction -> ControlSpec(0, 14, \lin, 1),
			\u_gain -> ControlSpec( -inf, 24, \db ),
			\u_mute -> BoolSpec( false ),
			\u_fadeIn -> ControlSpec( 0, inf ),
			\u_fadeOut -> ControlSpec( 0, inf )
			
		]);
	}
	
	*kr { |gain = 0, fadeIn = 0, fadeOut = 0, extraSilence = 0, useGlobalGain = 1|
		var name = this.getControlName( );
		var gate = this.getControl( \kr, name, 'gate', 1 );
		var mute = this.getControl( \kr, name, 'mute', 0 );
		var dur = this.getControl( \kr, name, 'dur', inf );
		var doneAction = this.getControl( \kr, name, 'doneAction', 0 );
		
		gain = this.getControl( \kr, name, 'gain', gain, 0.5 ); // 0.5s lag time
		fadeIn = this.getControl( \kr, name, 'fadeIn', fadeIn );
		fadeOut = this.getControl( \kr, name, 'fadeOut', fadeOut );
		
		^DemandEnvGen.kr( 
				Dseq( [ 0, 1, 1, 0, 0 ], 1 ), 
				Dseq( [ fadeIn, dur - (fadeIn+fadeOut), fadeOut, extraSilence ], 1 ),
				doneAction: doneAction ) *
			Env([ 1, 0, 0 ],[ fadeOut, extraSilence ], \lin, 0 )
				.kr( doneAction, RunningMin.kr( gate + Impulse.kr(0) ) ) *
			UGlobalGain.kr( useGlobalGain ) * gain.dbamp * (1-mute);
	}
	
	*ar { ^this.shouldNotImplement }
}


