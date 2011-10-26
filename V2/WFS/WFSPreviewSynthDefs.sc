WFSPreviewSynthDefs {
	
	/*
	These synthdefs should be placed after a WFSPrePanSynthDef. 
	They mimick the sound of WFSArrayPanSynthDefs. The preview might
	not be completely accurate.
	*/
	
	classvar <>synthDefs;
	
	classvar <>modes;
	classvar <>pannerFuncs;
	
	*initClass {
		modes = [ \s, \d ]; // static, dynamic
		
		pannerFuncs = (
			\headphone: { |in, point|
				// simple headphone panner (ear distance 0.19cm)
				// no HRTFs involved (yet..)
				var distances, globalDist, delays, amplitudes;
				distances = [ -0.095@0, 0.095@0 ].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = 0.06 + ((distances - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.1, delays );
				amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			},
			\stereo: { |in, point|
				var distances, globalDist, delays, amplitudes;
				distances = [ -0.5@0, 0.5@0 ].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = 0.06 + ((distances - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = Pan2.kr( 1, (point.x / 5).clip(-1,1) );
				in * amplitudes;
			}
		);
	}
	
	*getDefName { |type = \headphone, mode = \s|
		^["wfsx", type, mode.asString[0].toLower ].join("_");
	}
	
	*generateDef { |type = \headphone, mode = \s|
		
		mode = mode.asString[0].toLower.asSymbol;
		
		^SynthDef( this.getDefName( type, mode ), {
			
			var point = 0@0, amp = 1;
			var input;
			
			amp = \amp.kr( amp );
			
			// depending on mode
			if( mode === \d ) {
				point = UIn.kr(0,2).asPoint;
			} {
				point = \point.ir([0,0]).asPoint;
			};
				
			input = UIn.ar(0, 1) * amp;
			
			Out.ar( 0, pannerFuncs[ type ].value( input, point ) );
		});
		
	}
	
	*generateAll { |dir|
		dir = dir ? SynthDef.synthDefDir;
		synthDefs = modes.collect({ |mode|
			pannerFuncs.keys.as(Array).collect({ |type|
				this.generateDef( type, mode ).writeDefFile( dir );
			})
		}).flatten(1);
		^synthDefs;		
	
	}
	
	
}