UScoreEditorGui_TransportBar {

    var <>views, <>scoreController;

    *new{ |parent, bounds, score|
        ^super.new.init(parent, bounds, score)
    }

    init{ |parent, bounds, score|
        this.makeGui(parent, bounds, score);
        this.addControllers(score);
    }

    addControllers{ |score|
        scoreController = SimpleController( score );

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

    }

    makeGui{ |parent, bounds, score|

        var font = Font( Font.defaultSansFace, 11 ), view, size, marginH, marginV;
		views = ();

		marginH = 2;
	    marginV = 2;
		size = bounds.height - (2*marginV);
        view = CompositeView( parent, bounds );

		view.addFlowLayout(marginH@marginV);
		//view.background_( Color.white );
		view.resize_(8);



        views[\prepare] = WaitView( view, size@size )
					.alphaWhenStopped_( 0 );

		views[\play] = SmoothButton( view, 40@size  )
			.states_( [
			    [ \play, Color.black, Color.clear ],
			    [ \stop, Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({  |v|
			    var startedPlaying;
			    if( v.value == 1) {
			        startedPlaying = score.start( UServerCenter.servers, score.pos);
			        if( startedPlaying) { views[\prepare].start }{ v.value = 0 };
			    } {
                    score.stop;
                    views[\pause].value = 0;
                    views[\prepare].stop;
			    }

			});
			
		views[\pause] = SmoothButton( view, 50@size  )
			.states_( [
			    [ \pause, Color.black, Color.clear ],
			    [ \pause, Color.blue, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.background_(Color.grey(0.8))
			.action_({ |v|
			    if( v.value == 1) {
			        if(score.isPlaying) {
			         score.pause;
			       } {
			        v.value = 0;
			       }
			    } {
                    score.resume(UServerCenter.servers)
			    }
			});

		views[\rewind] = SmoothButton( view, 50@size  )
			.states_( [[ "<<", Color.black, Color.clear ]])
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    score.pos = 0;
			});

    }

}