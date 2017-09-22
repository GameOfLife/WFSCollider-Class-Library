WFSPreviewSynthDefs : AbstractWFSSynthDefs {
	
	/*
	These synthdefs should be placed after a WFSPrePanSynthDef. 
	They mimick the sound of WFSArrayPanSynthDefs. The preview might
	not be completely accurate.
	*/
	
	classvar <>modes;
	classvar <>pannerFuncs;
	classvar <>panDist = 0.2;
	
	*prefix { ^"wfsx" }
	
	*initClass {
		modes = [ \s, \d ]; // static, dynamic
		
		pannerFuncs = (
			\headphone: { |in, point|
				// simple headphone panner (ear distance 0.19cm)
				// no HRTFs involved (yet..)
				var distances, globalDist, delays, amplitudes;
				distances = [ -0.095@0, 0.095@0 ].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = ((distances + 0.095 - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.1, delays );
				amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			},
			\stereo: { |in, point|
				var distances, globalDist, delays, amplitudes;
				distances = [ -0.3@0, 0.3@0 ].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = ((distances + 0.3 - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			},
			\quad: { |in, point| // clockwise quadraphonic panning
				var distances, globalDist, delays, amplitudes;
				var radius = panDist; // should be < 1
				distances = [ 
					(radius.neg)@radius, radius@radius, 
					radius@(radius.neg), (radius.neg)@(radius.neg)
				].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = PanAz.kr( 4, 1, (point.angle - 0.5pi).neg / pi);
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			},
			\quad_crossed: { |in, point| // quadraphonic panning L, R, Lb, Rb
				var distances, globalDist, delays, amplitudes;
				var radius = panDist; // should be < 1
				distances = [ 
					(radius.neg)@radius, radius@radius, 
					radius@(radius.neg), (radius.neg)@(radius.neg)
				].collect(_.dist( point ));
				globalDist = (0@0).dist( point );
				delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = PanAz.kr( 4, 1, (point.angle - 0.5pi).neg / pi);
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				(in * amplitudes)[[0,1,3,2]];
			},
			\hexa: { |in, point| // clockwise hexaphonic panning, 
				var distances, globalDist, delays, amplitudes;
				var radius = panDist; // should be < 1
				distances = ((2,1..-3)*2pi/6).collect({ |item|
					Polar(radius,item).asPoint.dist( point )
				});
				globalDist = (0@0).dist( point );
				delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = PanAz.kr( 6, 1, (point.angle - ((2/3)*pi)).neg / pi, orientation: 0);
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			},
			\octo: { |in, point| // clockwise octophonic panning, first speaker straight front
				var distances, globalDist, delays, amplitudes;
				var radius = panDist; // should be < 1
				distances = ((2,1..-5)*2pi/8).collect({ |item|
					Polar(radius,item).asPoint.dist( point )
				});
				globalDist = (0@0).dist( point );
				delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
				in = DelayC.ar( in, 0.12, delays );
				amplitudes = PanAz.kr( 8, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
				amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
				in * amplitudes;
			}
		);
	}
	
	*getDefName { |type = \headphone, mode = \s|
		^[ this.prefix, type, mode.asString[0].toLower ].join("_");
	}
	
	*generateDef { |type = \headphone, mode = \s|
		
		mode = mode.asString[0].toLower.asSymbol;
		
		^SynthDef( this.getDefName( type, mode ), {
			
			var point = 0@0, amp = 1;
			var input;
			
			amp = \amp.kr( amp );
			
			// depending on mode
			if( mode === \d ) {
				point = \point.kr([0,0]).asPoint;
			} {
				point = \point.ir([0,0]).asPoint;
			};
				
			input = UIn.ar(0, 1) * amp;
			
			Out.ar( \out.kr(0), pannerFuncs[ type ].value( input, point ) );
		});
		
	}
	
	*generateAll { |action, dir|
		dir = dir ? SynthDef.synthDefDir;
		synthDefs = modes.collect({ |mode|
			pannerFuncs.keys.as(Array).collect({ |type|
				this.generateDef( type, mode ).justWriteDefFile( dir );
			})
		}).flatten(1);
		action.value(this);
		^synthDefs;		
	
	}
	
	
}