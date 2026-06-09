WFSArrayPanDirSynthDefs : WFSArrayPanSynthDefs {

	*prefix { ^"wfsd" }

	*generateDef { |size = 8, type = \uni, mode = \static, int = \n, sub = true|
		var conf;

		#type, mode, int = [ type, mode, int ].collect({ |item, i|
			var out;
			out = item.asString[0].toLower.asSymbol;
			if( [ types, modes, intTypes ][i].includes( out ).not ) {
				"WFSArrayPanSynth.generateDef - nonexistent %: %"
					.format( [\type, \mode, \int][i], item )
					.warn;
			};
			out;
		});

		^SynthDef( this.getDefName(size, type, mode, int, sub), {

			// synth args:
			var arrayConf, outOffset = 0, addDelay = 0;
			var point = 0@0, amp = 1, arrayRollOff = -9, arrayLimit = 1, arraySoftLimit = 0.5;
			var radiation = [0,1,0,1], direction = 0;
			var subSpacing = 16, subOffset = 11, subOutBus = -1, subFreq = 70, subGain = 0;

			// local variables
			var gain = 0.dbamp; // hard-wired for now
			var panner, input, subSig, subPanner;

			// always static
			arrayConf = \arrayConf.ir( [ size, 5, 0.5pi, 0, 0.164 ] ); // size is fixed in def
			outOffset = \outOffset.ir( outOffset );
			addDelay = \addDelay.ir( addDelay );

			// depending on mode
			if( mode === \d ) {
				point = \point.kr([0,0]).asPoint;
			} {
				point = \point.ir([0,0]).asPoint;
			};

			amp = \amp.kr(amp);

			arrayRollOff = \arrayDbRollOff.ir( arrayRollOff );
			arrayLimit = \arrayLimit.ir( arrayLimit );
			arraySoftLimit = \arraySoftLimit.ir( arraySoftLimit );

			radiation = \radiation.kr( radiation );
			direction = \direction.kr( direction );

			gain = \gain.kr( gain );
			input = UIn.ar(0, 1) * gain * amp;

			if( sub ) {
				subSpacing = \subSpacing.ir( subSpacing );
				subOffset = \subOffset.ir( subOffset );
				subOutBus = \subOutBus.ir( subOutBus );
				subFreq = \subFreq.ir( subFreq );
				subGain = \subGain.ir( subGain );

				subSig = LRHiCut.ar( input, subFreq );
				input = LRLowCut.ar( input, subFreq );
			};

			panner = WFSArrayPanDir( size, *arrayConf[1..] )
				.addDelay_( addDelay )
				.dbRollOff_( arrayRollOff )
				.limit_( arrayLimit )
				.focusWidth_( \focusWidth.ir( 0.5pi ) )
				.focus_( switch( type, \f, { true }, \n, { false }, { nil } ) );

			if( sub ) {
				subPanner = panner.asSubArray( subSpacing, subOffset, 16 )
				.addDelay_( addDelay )
				.dbRollOff_( arrayRollOff )
				.limit_( arrayLimit )
				.softLimitRange_( arraySoftLimit )
				.focusWidth_( \focusWidth.ir( 0.5pi ) )
				.focus_( switch( type, \f, { true }, \n, { false }, { nil } ) );

				subSig = subPanner.ar( subSig, point, int, direction, radiation[..2], radiation[3], taper: false );
				subSig = subSig * subSig.size / size * subGain.dbamp;
				subSig.do({ |item,i|
					Out.ar(
						if( subOutBus < 0,
							outOffset + subOffset + (i * subSpacing),
							subOutBus + i
						), item
					);
				});
			};

			Out.ar( outOffset,
				panner.ar(
					input, point, int,
					direction, radiation[..2], radiation[3]
				)
			);
		});
	}

	*generateAll { |action, dir, estimatedTime = 90| // and write to disk

		// this takes about 30 seconds in normal settings
		// can be stopped via cmd-.

		var all, waitTime;
		dir = dir ? defaultDir ? SynthDef.synthDefDir;
		all = #[ // these are the all types we'll probably need
			[ uni, static, n, false ],    // use this for any static
			[ normal, static, n, false ], // use this for normal static
			[ focus, dynamic, l, false ],
			[ normal, dynamic, l, false ],
			[ focus, dynamic, c, false ],
			[ normal, dynamic, c, false ],

			[ uni, static, n, true ],    // use this for any static
			[ normal, static, n, true ], // use this for normal static
			[ focus, dynamic, l, true ],
			[ normal, dynamic, l, true ],
			[ focus, dynamic, c, true ],
			[ normal, dynamic, c, true ],
		];
		waitTime = estimatedTime / all.size;

		// now we generate them:
		{
			var started;
			started = Main.elapsedTime;
			"started generating WFSArrayPanSynthDefs".postln;
			" this may take % seconds or more\n".postf( estimatedTime );
			synthDefs = all.collect({ |item|
				var out = this.allSizes.collect({ |size|
					this.generateDef(size, *item )
						.justWriteDefFile( dir );
				});
				waitTime.wait;
				"  WFSArrayPanDirSynthDefs synthdefs for % ready\n".postf( item.join("_") );
				out;
			});
			"done generating WFSArrayPanDirSynthDefs in %s\n"
				.postf( (Main.elapsedTime - started).round(0.001) );
			action.value( synthDefs );
		}.fork(AppClock);
	}
}
