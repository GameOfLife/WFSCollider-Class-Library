WFSPreviewSynthDefs : AbstractWFSSynthDefs {

	/*
	These synthdefs should be placed after a WFSPrePanSynthDef.
	They mimick the sound of WFSArrayPanSynthDefs. The preview might
	not be completely accurate.
	*/

	classvar <>modes;
	classvar <>types;
	classvar <>pannerFuncs;
	classvar <>panDist = 0.2;

	*prefix { ^"wfsx" }

	*initClass {
		modes = [ \s, \d ]; // static, dynamic
		types = [ \n, \p ]; // normal (point), plane
		pannerFuncs = (
			\n: ( // point
				\headphone: { |in, point|
					// simple headphone panner (ear distance 0.19cm)
					// no HRTFs involved (yet..)
					var distances, globalDist, delays, amplitudes;
					globalDist = (0@0).dist( point );
					distances = BinauralDistance( globalDist, point.angle.neg, 0.18/2 )[[0,1]];
					delays = ((distances + 0.09 - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.1, delays + ControlDur.ir);
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.75pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\binaural: { |in, point, elevation|
					var distances, globalDist, delays, angles, ambis;
					globalDist = (0@0).dist( point );
					distances = BinauralDistance( globalDist,
						this.getFlatAngle( point.angle, elevation ).neg,
						0.18/2
					)[[0,1]];
					delays = ((distances + 0.09 - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.1, delays + ControlDur.ir);
					angles = [ -0.09@0, 0.09@0 ].collect({ |ear|
						(point - ear).angle - 0.5pi
					});
					ambis = angles.collect({ |angle, i|
						PanB.ar( in[i] * 0.25, angle.neg / pi, elevation / 0.5pi );
					});
					UPrivateOut.ar( 0, ambis.flatten(1) );
					DC.ar(0.dup);
				},
				\stereo: { |in, point|
					var distances, globalDist, delays, amplitudes;
					distances = [ -0.3@0, 0.3@0 ].collect(_.dist( point ));
					globalDist = (0@0).dist( point );
					delays = ((distances + 0.3 - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				'2.1': { |in, point| // L, R, (silent), Sub
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = [ -0.3@0, 0.3@0, 0@0 ].collect(_.dist( point ));
					globalDist = (0@0).dist( point );
					delays = ((distances + 0.3 - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in[[0,1]] * amplitudes) ++ [ Silent.ar, LPF.ar( in[2], 120 ) ])
				},
				'3.1': { |in, point| // L, R, C, Sub
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = [ -0.3@0, 0.3@0, 0@0 ].collect(_.dist( point ));
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 3, 1, ((point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi) * 2/3, orientation: 0);
					amplitudes = amplitudes[[2,1,0]];
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in * amplitudes) ++ [ LPF.ar( in[2], 120 ) ])
				},
				\lrs: { |in, point| // left, right, surround/back
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = [
						(radius.neg)@radius, radius@radius,
						0@(radius.neg)
					].collect(_.dist( point ));
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 3, 1, (point.angle - (0.5pi)).neg / pi);
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
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
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
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 4, 1, (point.angle - 0.5pi).neg / pi);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					(in * amplitudes)[[0,1,3,2]];
				},
				'4.1': { |in, point| // L, R, (silent), Sub, Ls, Rs
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = [
						(radius.neg)@radius, radius@radius,
						radius@(radius.neg), (radius.neg)@(radius.neg), 0@0
					].collect(_.dist( point ));
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 4, 1, (point.angle - 0.5pi).neg / pi);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in[..3] * amplitudes) ++ [ Silent.ar, LPF.ar( in[4], 120 ) ])[[0,1,4,5,3,2]];
				},
				'5.1': { |in, point| // L, R, C, Sub, Ls, Rs
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..4) * -2pi/5).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					}) ++ [ (0@0).dist( point ) ];
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 5, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in[..4] * amplitudes) ++ [ LPF.ar( in[5], 120 ) ])[[ 4, 1, 0, 5, 3, 2 ]];
				},
				\hexa: { |in, point| // clockwise hexaphonic panning, first two speakers left and right of front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((2,1..-3)*2pi/6).collect({ |item|
						Polar(radius,item).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 6, 1, (point.angle - ((2/3)*pi)).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\hexa_pairs: { |in, point| // pair-wise hexaphonic panning, stereo pairs front to back
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((2,1..-3)*2pi/6).collect({ |item|
						Polar(radius,item).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 6, 1, (point.angle - ((2/3)*pi)).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					(in * amplitudes)[[ 0, 1, 5, 2, 4, 3 ]];
				},
				\hepta: { |in, point| // clockwise 7-speaker panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..6) * -2pi/7).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 7, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				'7.1': { |in, point| // L, R, C, Sub, Lm, Rm, Ls, Rs
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..6) * -2pi/7).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					}) ++ [ (0@0).dist( point ) ];
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 7, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in[..6] * amplitudes) ++ [ LPF.ar( in[7], 120 ) ])[[ 6, 1, 0, 7, 5, 2, 4, 3 ]];
				},
				\octo: { |in, point| // clockwise octophonic panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..7) * -2pi/8).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 8, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\octo_pairs: { |in, point| // pair-wise octophonic panning, stereo pairs from front to back
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..7) * -2pi/8).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 8, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					(in * amplitudes)[[ 0, 1, 7, 2, 6, 3, 5, 4 ]];
				},
				\duo_deci: { |in, point| // clockwise 16-channel panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..11) * -2pi/12).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 12, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\hexa_deci: { |in, point| // clockwise 16-channel panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..15) * -2pi/16).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 16, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\twentyfour: { |in, point| // clockwise 24-channel panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..23) * -2pi/24).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 24, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\thirtytwo: { |in, point| // clockwise 32-channel panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..31) * -2pi/32).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 32, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\sixtyfour: { |in, point| // clockwise 64-channel panning, first speaker straight front
					var distances, globalDist, delays, amplitudes;
					var radius = panDist; // should be < 1
					distances = ((..63) * -2pi/64).collect({ |item|
						Polar(radius,item + 0.5pi).asPoint.dist( point )
					});
					globalDist = (0@0).dist( point );
					delays = ((distances + radius - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.12, delays + ControlDur.ir);
					amplitudes = PanAz.kr( 64, 1, (point.angle - 0.5pi).neg / pi, orientation: 0);
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\b_format: { |in, point| // 1st order b-format output (2D; 3 channels)
					PanB2.ar( in, (point.angle - 0.5pi).neg / pi);
				},
				\ambix: { |in, point, elevation| // 1st order b-format output WYZX (4-channels) with SN3D normalization
					var hoa;
					WFSLib.ifATK {
						hoa = HoaEncodeDirection.ar( in * 0.5.sqrt,
							point.angle - 0.5pi, elevation, 1.5, 1
						);
						HoaDecodeMatrix.ar( hoa,
							HoaMatrixDecoder.newFormat( \ambix, 1 )
						);
					} {
						PanB.ar( in * 0.5.sqrt,
							(point.angle - 0.5pi).neg / pi, elevation / 0.5pi
						)[ [ 0, 2, 3, 1 ] ] * [ 1, 0.5.sqrt, 0.5.sqrt, 0.5.sqrt ];
					};
				},
				\mono: { |in, point|
					in;
				}
			),
			\p: ( // plane
				\headphone: { |in, point|
					// simple headphone panner (ear distance 0.19cm)
					// no HRTFs involved (yet..)
					var globalDist, amplitudes;
					globalDist = (0@0).dist( point );
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.75pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				\binaural: { |in, point, elevation|
					var distances, globalDist, delays, angles, ambis;
					globalDist = 10;
					point = point.asPolar.rho_(10).asPoint;
					distances = BinauralDistance( globalDist,
						this.getFlatAngle( point.angle, elevation ).neg,
						0.18/2
					)[[0,1]];
					delays = ((distances + 0.09 - globalDist) / WFSBasicPan.speedOfSound);
					in = DelayC.ar( in, 0.1, delays + ControlDur.ir);
					angles = [ -0.09@0, 0.09@0 ].collect({ |ear|
						(point - ear).angle - 0.5pi
					});
					ambis = angles.collect({ |angle, i|
						PanB.ar( in[i] * 0.25, angle.neg / pi, elevation / 0.5pi );
					});
					UPrivateOut.ar( 0, ambis.flatten(1) );
				},
				\stereo: { |in, point|
					var globalDist, amplitudes;
					globalDist = (0@0).dist( point );
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					in * amplitudes;
				},
				'2.1': { |in, point| // L, R, (silent), Sub
					var globalDist, amplitudes;
					globalDist = (0@0).dist( point );
					amplitudes = Pan2.kr( 1, (point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi );
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					(in * amplitudes) ++ [ DC.ar(0), LPF.ar( in, 120 ) ]
				},
				'3.1': { |in, point| // L, R, C, Sub
					var globalDist, amplitudes;
					globalDist = (0@0).dist( point );
					amplitudes = PanAz.kr( 3, 1, ((point.angle - 0.5pi).neg.fold(-0.5pi,0.5pi) / 0.5pi) * 2/3, orientation: 0);
					amplitudes = amplitudes[[2,1,0]];
					amplitudes = amplitudes.max( globalDist.linlin(0.5,1,1,0).clip(0,1) );
					((in * amplitudes) ++ [ LPF.ar( in, 120 ) ])
				},
				\lrs: { |in, point| // left, right, surround/back
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 3, w, x, y, 0.5 );
				},
				\quad: { |in, point| // clockwise quadraphonic panning, AEP
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 4, w, x, y, 0.5 );
				},
				\quad_crossed: { |in, point| // quadraphonic panning L, R, Lb, Rb
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 4, w, x, y, 0.5 )[[0,1,3,2]];
				},
				'4.1': { |in, point| // L, R, (silent), Sub, Lm, Rm, Ls, Rs
					var w,x,y,z, panned;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					panned = DecodeB2.ar( 4, w, x, y, 0.5 );
					(panned ++ [ Silent.ar, LPF.ar( in, 120 ) ])[[0,1,4,5,3,2]];
				},
				'5.1': { |in, point| // L, R, C, Sub, Lm, Rm, Ls, Rs
					var w,x,y,z, panned;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					panned = DecodeB2.ar( 5, w, x, y, 0 );
					(panned ++ [ LPF.ar( in, 120 ) ])[[ 4, 1, 0, 5, 3, 2 ]];
				},
				\hexa: { |in, point| // clockwise hexaphonic AEP panning, first two speakers left and right of front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 6, w, x, y, 0.5 );
				},
				\hexa_pairs: { |in, point| // pairwise hexaphonic AEP panning, stereo pairs from front to back

					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 6, w, x, y, 0.5 )[[ 0, 1, 5, 2, 4, 3 ]];
				},
				\hepta: { |in, point| // clockwise 7-speaker AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 7, w, x, y, 0 );
				},
				'7.1': { |in, point| // L, R, C, Sub, Lm, Rm, Ls, Rs
					var w,x,y,z, panned;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					panned = DecodeB2.ar( 7, w, x, y, 0 );
					(panned ++ [ LPF.ar( in, 120 ) ])[[ 6, 1, 0, 7, 5, 2, 4, 3 ]];
				},
				\octo: { |in, point| // clockwise octophonic AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 8, w, x, y, 0 );
				},
				\octo_pairs: { |in, point| // pairwise octophonic AEP panning, stereo pairs from front to back
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 8, w, x, y, 0 )[[ 0, 1, 7, 2, 6, 3, 5, 4 ]];
				},
				\duo_deci: { |in, point| // clockwise 16-channel AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 12, w, x, y, 0 );
				},
				\hexa_deci: { |in, point| // clockwise 16-channel AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 16, w, x, y, 0 );
				},
				\twentyfour: { |in, point| // clockwise 24-channel AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 24, w, x, y, 0 );
				},
				\thirtytwo: { |in, point| // clockwise 32-channel AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 32, w, x, y, 0 );
				},
				\sixtyfour: { |in, point| // clockwise 64-channel AEP panning, first speaker straight front
					var w,x,y,z;
					#w,x,y,z = PanB.ar( in, (point.angle - 0.5pi).neg / pi, (0@0).dist( point ).linlin( 0,2,1,0,\minmax ) );
					DecodeB2.ar( 64, w, x, y, 0 );
				},
				\b_format: { |in, point| // 1st order b-format output (2D; 3 channels)
					PanB2.ar( in, (point.angle - 0.5pi).neg / pi);
				},
				\ambix: { |in, point, elevation| // 1st order b-format output WYZX (4-channels) with SN3D normalization
					var hoa;
					WFSLib.ifATK {
						hoa = HoaEncodeDirection.ar( in * 0.5.sqrt,
							point.angle - 0.5pi,
							elevation, 1.5, 1
						);
						HoaDecodeMatrix.ar( hoa,
							HoaMatrixDecoder.newFormat( \ambix, 1 )
						);
					} {
						PanB.ar( in * 0.5.sqrt,
							(point.angle - 0.5pi).neg / pi,
							elevation / 0.5pi
						)[ [ 0, 2, 3, 1 ] ] * [ 1, 0.5.sqrt, 0.5.sqrt, 0.5.sqrt ];
					};
				},
				\mono: { |in, point|
					in;
				}
			)
		);
		WFSLib.ifATK {
			[2,3,4,5,6,7].collect({ |order|
				var func;
				func = { |in, point, elevation|
					var hoa;
					hoa = HoaEncodeDirection.ar( in * 0.5.sqrt,
						point.angle - 0.5pi, elevation, 1.5, order
					);
					HoaDecodeMatrix.ar( hoa,
						HoaMatrixDecoder.newFormat( \ambix, order )
					);
				};
				pannerFuncs[ \n ].put( "ambix_%o".format( order ).asSymbol, func );
				pannerFuncs[ \p ].put( "ambix_%o".format( order ).asSymbol, func );
			});
			[3,5,7].collect({ |order|
				var func;
				func = { |mode = \n|
					{ |in, point, elevation|
						var distances, globalDist, delays, angles, ambis;
						if( mode == \n ) {
							globalDist = (0@0).dist( point );
						} {
							globalDist = 10; // plane wave is always 10m away
							point = point.asPolar.rho_(10).asPoint;
						};
						distances = BinauralDistance( globalDist,
							this.getFlatAngle( point.angle, elevation ).neg,
							0.18/2
						)[[0,1]];
						delays = ((distances + 0.09 - globalDist) / WFSBasicPan.speedOfSound);
						in = DelayC.ar( in, 0.1, delays + ControlDur.ir);
						angles = [ -0.09@0, 0.09@0 ].collect({ |ear|
							(point - ear).angle - 0.5pi
						});
						ambis = angles.collect({ |angle, i|
							HoaEncodeDirection.ar( in[i] * 0.25,
								angle, elevation, 1.5, order
							); // output in acn-n3d
						});
						UPrivateOut.ar( 0, ambis.flatten(1) );
						DC.ar(0.dup);
					};
				};
				pannerFuncs[ \n ].put( "binaural_%o".format( order ).asSymbol, func.value( \n ) );
				pannerFuncs[ \p ].put( "binaural_%o".format( order ).asSymbol, func.value( \p ) );
			})
		};
	}

	*getFlatAngle { |theta = 0, phi = 0|
		var x,y,z, cosphi, tilt, ty;
		cosphi = cos(phi);
		x = cos(theta) * cosphi;
		y = sin(theta) * cosphi;
		z = sin(phi);
		tilt = atan2(z, y).neg;
		ty = (y * tilt.cos) - (z * tilt.sin);
		^atan2(ty, x);
	}

	*getDefName { |which = \headphone, mode = \s, type = \n|
		^[ this.prefix, which, mode.asString[0].toLower, type ].join("_");
	}

	*generateDef { |which = \headphone, mode = \s, type = \n|

		mode = mode.asString[0].toLower.asSymbol;

		^SynthDef( this.getDefName( which, mode, type ), {

			var point = 0@0, amp = 1;
			var dbRollOff = -6, limit = 5, latencyComp = 0, pointLag = 0;
			var input, output, rho, env, panner;
			var sendPointRate = 0;
			var elevation;

			amp = \amp.kr( amp );

			// depending on mode
			if( mode === \d ) {
				point = \point.kr([0,0]);
				SendReply.kr( Impulse.kr( \sendPointRate.kr(sendPointRate) ), '/point', point );
			} {
				point = \point.ir([0,0]);
				SendReply.kr( Impulse.kr( 0 ), '/point', point );
			};

			point = point.asPoint;
			rho = point.rho;

			input = UIn.ar(0, 1) * amp;

			if( type == \p ) { dbRollOff = 0 };

			dbRollOff = \dbRollOff.kr( dbRollOff );
			limit = \maxAmpRadius.kr( limit );
			latencyComp = \latencyComp.ir( latencyComp );
			elevation = \elevation.kr( 0 );

			// the pre-panner and delayed/attenuated output
			panner = WFSPrePan( dbRollOff, limit, latencyComp );

			// filter and clip input for speaker protection
			//input = LeakDC.ar( input, 0.997 );
			input = UGlobalEQ.ar( input );
			input = (input / 4).softclip * 4; // 6dB headroom, then softclip to 12dB
			input = OnePole.ar( input,
				( -2pi * ( ( 100000 / ( rho * \distanceFilter.kr(0).cubed ) )
					.clip(0,10000000) / SampleRate.ir )
				).exp
			);
			env = UEnv.kr( extraSilence:
				( ( rho / WFSBasicPan.speedOfSound ) * (1 - latencyComp) ) + 0.12
			);
			input = input * env;
			input = panner.ar( input, point );
			output = pannerFuncs[ type ][ which ].value( input, point, elevation );

			// end from prepan

			Out.ar( \out.kr(0), output );
		});

	}

	*generateAll { |action, dir|
		dir = dir ? defaultDir ? SynthDef.synthDefDir;
		synthDefs = modes.collect({ |mode|
			types.collect({ |type|
				pannerFuncs[ type ].keys.as(Array).collect({ |which|
					this.generateDef( which, mode, type ).justWriteDefFile( dir );
				})
			})
		}).flatten(2);
		action.value(this);
		^synthDefs;

	}


}