UScoreEditorGui_TransportBar {
    var <scoreView;
    var <>views, <>scoreController, <scoreViewController;

    *new{ |parent, bounds, scoreView|
        ^super.newCopyArgs(scoreView).init(parent, bounds)
    }

    init{ |parent, bounds|
        this.makeGui(parent, bounds);
        scoreViewController = SimpleController( scoreView );
        scoreViewController.put(\scoreChanged, {
		    this.addControllers;
		});
        this.addControllers;
    }

    score{
        ^scoreView.currentScore
    }

    addControllers{
        if(scoreController.notNil) {
            scoreController.remove;
        };
        scoreController = SimpleController( this.score );

		scoreController.put(\preparing, {
		    { views[\prepare].start }.defer;
		});

		scoreController.put(\playing, {
		    { views[\prepare].stop }.defer;
		});

		scoreController.put(\stop, {

            {views[\prepare].stop;
            views[\pause].value = 0;
            views[\play].value = 0; }.defer;

		});

		scoreController.put(\resumed, {
            {views[\pause].value = 0;}.defer;
		});

		scoreController.put(\paused, {
            {views[\pause].value = 1;}.defer;
		});

		scoreController.put(\start, {
            {views[\play].value = 1;}.defer;
		});

		scoreController.put(\pos, { |who,what,pos|
            {
            views[\counter].value = pos;
            }.defer;
		});

		views[\play].value = this.score.isPlaying.binaryValue;
		views[\pause].value = this.score.isPaused.binaryValue;
		if(this.score.isPreparing) {
		    views[\prepare].start
		} {
		    views[\prepare].stop
		}


    }

    makeGui{ |parent, bounds|

        var font = Font( Font.defaultSansFace, 11 ), view, size, marginH, marginV, playAlt;
		views = ();

		marginH = 2;
	    marginV = 2;
		size = bounds.height - (2*marginV);
        view = CompositeView( parent, bounds );

		view.addFlowLayout(marginH@marginV);
		//view.background_( Color.white );
		view.resize_(8);



        views[\prepare] = WaitView( view, size@size )
					.alphaWhenStopped_( 0 )
					.canFocus_(false);

		views[\play] = SmoothButton( view, 40@size  )
			.states_( [
			    [ \play, Color.black, Color.clear ],
			    [ \stop, Color.black, Color(0.40298507462687, 0.73134328358209, 0.44776119402985) ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({  |v,c,d,e|

			    var startedPlaying;
			    if( v.value == 1) {
                    startedPlaying = this.score.prepareAndStart( UServerCenter.servers, this.score.pos);
			        if( startedPlaying.not ){ v.value = 0 };
			    } {
                    this.score.stop;
                    views[\pause].value = 0;
                    views[\prepare].stop;
			    }

			});
			
		views[\pause] = SmoothButton( view, 50@size  )
			.states_( [
			    [ \pause, Color.black, Color.clear ],
			    [ \pause, Color.red,Color(0.40298507462687, 0.73134328358209, 0.44776119402985) ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.background_(Color.grey(0.8))
			.action_({ |v|
			    if( v.value == 1) {
			        if(this.score.isPlaying) {
			         this.score.pause;
			       } {
			        v.value = 0;
			       }
			    } {
                    this.score.resume(UServerCenter.servers)
			    }
			});

		views[\return] = SmoothButton( view, 50@size  )
			.states_( [[\return, Color.black, Color.clear ]])
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    this.score.pos = 0;
			});

        view.decorator.shift(20,0);

	    views[\counter] = SMPTEBox( view, 150@size )
			.value_( this.score.pos )
			.radius_( 12 )
			.align_( \center )
			.clipLo_(0)
			.background_( Color.clear )
			.charSelectColor_( Color.white.alpha_(0.5) )
			.autoScale_( true )
            .action_({ |v| this.score.pos = v.value });

    }

}