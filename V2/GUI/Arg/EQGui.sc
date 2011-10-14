EQGui {
    var eq;

    *new{  |parent, bounds, label, action, resize|
        ^super.new.init(parent, bounds, label, action, resize)
    }

    init { |parent, bounds, label, action, resize|
        /*^UserView( parent,
        bounds//200@100
        ).resize_(resize).background_(Color.black);*/

        var composite = CompositeView(parent,305@220);
        composite.decorator = FlowLayout( composite.bounds, 10@10, 4@0 );
        eq = ();
        eq[\send_current] = {
            action.value( this, EQArg(eq[\frdb]) )
        };
        eq[ \frdb ] = EQSpec().defaultArray;
        eq[ \uvw ] = UserView( composite,
                    composite.bounds.insetBy(10,10)
                        .height_(composite.bounds.height - 80)
                    ).resize_(5);
        eq[ \uvw ].focusColor = Color.clear;
        eq[ \font ] = Font( Font.defaultSansFace, 10 );
        eq[ \tvw ] = TabbedView( composite,
            composite.bounds.insetBy(10,10).height_(35).top_(200),
        [ "low shelf", "peak 1", "peak 2", "peak 3", "high shelf" ],
        { |i| Color.hsv( i.linlin(0,5,0,1), 0.75, 0.5).alpha_( 0.25 ); }!5 )
            .font_( eq[ \font ] )
            .resize_( 8 )
            .tabPosition_( \bottom );
        //eq[ \tvw ].focusActions = { |i| { eq[ \selected ] = i; eq[ \uvw ].refresh;  }; }!5;
        eq[ \tvw_views ] = [];

        composite.decorator.shift(0,8);

		eq[ \tvw ].views.do({ |view,i|
			var vw_array = [];

			view.decorator = FlowLayout( view.bounds.moveTo(0,0) );

			StaticText( view, 35@14 ).font_( eq[ \font ] ).align_( \right ).string_( "freq:" );
			vw_array = vw_array.add(
				RoundNumberBox( view, 40@14 ).font_( eq[ \font ] ).value_( eq[ \frdb ][i][0] )
					.clipLo_(20).clipHi_(22000)
					.action_({ |vw|
						eq[ \frdb ][i][0] = vw.value;
						eq[ \send_current ].value;
						eq[ \uvw ].refresh;
						eq[ \pumenu_check ].value;
						})  );

			StaticText( view, 25@14 ).font_( eq[ \font ] ).align_( \right ).string_( "db:" );
			vw_array = vw_array.add(
				RoundNumberBox( view, 40@14 ).font_( eq[ \font ] ).value_( eq[ \frdb ][i][1] )
					.clipLo_( -36 ).clipHi_( 36 )
					.action_({ |vw|
						eq[ \frdb ][i][1] = vw.value;
						eq[ \send_current ].value;
						eq[ \uvw ].refresh;
						eq[ \pumenu_check ].value;
						})  );

			StaticText( view, 25@14 ).font_( eq[ \font ] ).align_( \right )
				.string_( (0: "rs:", 4:"rs:")[i] ? "rq"  );
			vw_array = vw_array.add(
				RoundNumberBox( view, 40@14 ).font_( eq[ \font ] ).value_( eq[ \frdb ][i][2] )
					.step_(0.1).clipLo_( if( [0,4].includes(i) ) { 0.6 } {0.01}).clipHi_(10)
					.action_({ |vw|
						eq[ \frdb ][i][2] = vw.value;
						eq[ \send_current ].value;
						eq[ \uvw ].refresh;
						eq[ \pumenu_check ].value;
						})
						);

			eq[ \tvw_views ] = eq[ \tvw_views ].add( vw_array );
            eq[ \uvw ].drawFunc = { |vw|
			var freqs, svals, values, bounds, zeroline;
			var freq = 1200, rq = 0.5, db = 12;
			var min = 20, max = 22050, range = 24;
			var vlines = [100,1000,10000];
			var dimvlines = [25,50,75, 250,500,750, 2500,5000,7500];
			var hlines = [-18,-12,-6,6,12,18];
			var pt, strOffset = 11;

			if( GUI.id === 'swing' ) { strOffset = 14 };

			bounds = vw.bounds.moveTo(0,0);

			#freq,db,rq = eq[ \frdb ][0] ? [ freq, db, rq ];

			freqs = ({|i| i } ! (bounds.width+1));
			freqs = freqs.linexp(0, bounds.width, min, max );

			values = [
				BLowShelf.magResponse( freqs, 44100, eq[ \frdb ][0][0], eq[ \frdb ][0][2],
					eq[ \frdb ][0][1]),
				BPeakEQ.magResponse( freqs, 44100, eq[ \frdb ][1][0], eq[ \frdb ][1][2],
					eq[ \frdb ][1][1]),
				BPeakEQ.magResponse( freqs, 44100, eq[ \frdb ][2][0], eq[ \frdb ][2][2],
					eq[ \frdb ][2][1]),
				BPeakEQ.magResponse( freqs, 44100, eq[ \frdb ][3][0], eq[ \frdb ][3][2],
					eq[ \frdb ][3][1]),
				BHiShelf.magResponse( freqs, 44100, eq[ \frdb ][4][0], eq[ \frdb ][4][2],
					eq[ \frdb ][4][1])
					].ampdb.max(-200).min(200);

			zeroline = 0.linlin(range.neg,range, bounds.height, 0, \none);

			svals = values.sum.linlin(range.neg,range, bounds.height, 0, \none);
			values = values.linlin(range.neg,range, bounds.height, 0, \none);

			vlines = vlines.explin( min, max, 0, bounds.width );
			dimvlines = dimvlines.explin( min, max, 0, bounds.width );

			pt = eq[ \frdb ].collect({ |array|
				(array[0].explin( min, max, 0, bounds.width ))
				@
				(array[1].linlin(range.neg,range,bounds.height,0,\none));
				});

				Pen.color_( Color.white.alpha_(0.25) );
				Pen.roundedRect( bounds, [6,6,0,0] ).fill;

				Pen.color = Color.gray(0.2).alpha_(0.5);

				Pen.roundedRect( bounds.insetBy(0,0), [6,6,0,0] ).clip;

				Pen.color = Color.gray(0.2).alpha_(0.125);

				hlines.do({ |hline,i|
					hline = hline.linlin( range.neg,range, bounds.height, 0, \none );
					Pen.line( 0@hline, bounds.width@hline )
					});
				dimvlines.do({ |vline,i|
					Pen.line( vline@0, vline@bounds.height );
					});
				Pen.stroke;

				Pen.color = Color.gray(0.2).alpha_(0.5);
				vlines.do({ |vline,i|
					Pen.line( vline@0, vline@bounds.height );
					});
				Pen.line( 0@zeroline, bounds.width@zeroline ).stroke;

				Pen.font = eq[ \font ];

				Pen.color = Color.gray(0.2).alpha_(0.5);
				hlines.do({ |hline|
					Pen.stringAtPoint( hline.asString ++ "dB",
						3@(hline.linlin( range.neg,range, bounds.height, 0, \none )
							- strOffset) );
					});
				vlines.do({ |vline,i|
					Pen.stringAtPoint( ["100Hz", "1KHz", "10KHz"][i],
						(vline+2)@(bounds.height - (strOffset + 1)) );
					});

				values.do({ |svals,i|
					var color;
					color = Color.hsv(
						i.linlin(0,values.size,0,1),
						0.75, 0.5).alpha_(if( eq[ \selected ] == i ) { 0.75 } { 0.25 });
					Pen.color = color;
					Pen.moveTo( 0@(svals[0]) );
					svals[1..].do({ |val, i|
						Pen.lineTo( (i+1)@val );
						});
					Pen.lineTo( bounds.width@(bounds.height/2) );
					Pen.lineTo( 0@(bounds.height/2) );
					Pen.lineTo( 0@(svals[0]) );
					Pen.fill;

					Pen.addArc( pt[i], 5, 0, 2pi );

					Pen.color = color.alpha_(0.75);
					Pen.stroke;

					});

				Pen.color = Color.blue(0.5);
				Pen.moveTo( 0@(svals[0]) );
				svals[1..].do({ |val, i|
					Pen.lineTo( (i+1)@val );
					});
				Pen.stroke;

				Pen.extrudedRect( bounds, [6,6,0,0], 1, inverse: true );
			};
	    });


        eq[ \uvw ].mouseDownAction = { |vw,x,y,mod|
            var bounds;
            var pt;
            var min = 20, max = 22050, range = 24;

            bounds = vw.bounds.moveTo(0,0);
            //pt = (x@y) - (bounds.leftTop);
            pt = (x@y);

            eq[ \selected ] =  eq[ \frdb ].detectIndex({ |array|
                (( array[ 0 ].explin( min, max, 0, bounds.width ) )@
                ( array[ 1 ].linlin( range.neg, range, bounds.height, 0, \none ) ))
                    .dist( pt ) <= 5;
                }) ? -1;

            if( eq[ \selected ] != -1 ) { eq[ \tvw ].focus( eq[ \selected ] ) };
            vw.refresh;
            };

        eq[ \uvw ].mouseMoveAction = { |vw,x,y,mod|
            var bounds;
            var pt;
            var min = 20, max = 22050, range = 24;

            bounds = vw.bounds.moveTo(0,0);
            //pt = (x@y) - (bounds.leftTop);
            pt = (x@y);

            if( eq[ \selected ] != -1 )
                {
                case { ModKey( mod ).alt }
                    {
                    if(  ModKey( mod ).shift )
                        {
                    eq[ \frdb ][eq[ \selected ]] = eq[ \frdb ][eq[ \selected ]][[0,1]]
                        ++ [ y.linexp( bounds.height, 0, 0.1, 10, \none ).nearestInList(
                            if( [0,4].includes(eq[ \selected ]) )
                                {[0.6,1,2.5,5,10]}
                                {[0.1,0.25,0.5,1,2.5,5,10]}

                            ) ];
                        }
                        {
                    eq[ \frdb ][eq[ \selected ]] = eq[ \frdb ][eq[ \selected ]][[0,1]]
                        ++ [ y.linexp( bounds.height, 0, 0.1, 10, \none ).clip(
                                 if( [0,4].includes(eq[ \selected ]) ) { 0.6 } {0.1},
                                    10).round(0.01) ];
                        };
                    eq[ \tvw_views ][eq[ \selected ]][2].value = eq[ \frdb ][eq[ \selected ]][2];
                         }
                    { ModKey( mod ).shift }
                    {
                eq[ \frdb ][eq[ \selected ]] = [
                    pt.x.linexp(0, bounds.width, min, max )
                        .nearestInList( [25,50,75,100,250,500,750,1000,2500,5000,7500,10000] ),
                    pt.y.linlin( 0, bounds.height, range, range.neg, \none )
                        .clip2( range ).round(6),
                    eq[ \frdb ][eq[ \selected ]][2]
                    ];
                eq[ \tvw_views ][eq[ \selected ]][0].value = eq[ \frdb ][eq[ \selected ]][0];
                eq[ \tvw_views ][eq[ \selected ]][1].value = eq[ \frdb ][eq[ \selected ]][1];
                    }
                    { true }
                    {
                eq[ \frdb ][eq[ \selected ]] = [
                    pt.x.linexp(0, bounds.width, min, max ).clip(20,20000).round(1),
                    pt.y.linlin( 0, bounds.height, range, range.neg, \none ).clip2( range )
                        .round(0.25),
                    eq[ \frdb ][eq[ \selected ]][2]
                    ];
                eq[ \tvw_views ][eq[ \selected ]][0].value = eq[ \frdb ][eq[ \selected ]][0];
                eq[ \tvw_views ][eq[ \selected ]][1].value = eq[ \frdb ][eq[ \selected ]][1];		};
            eq[ \send_current ].value;
            vw.refresh;
            eq[ \pumenu_check ].value;
                };

            };

    }

    setValue { |eqArg, active = false|
		//view.value = value;
		//if( active ) { view.doAction };
		eq[\frdb] = eqArg.values;
		eq[ \uvw ].refresh;
	}


}