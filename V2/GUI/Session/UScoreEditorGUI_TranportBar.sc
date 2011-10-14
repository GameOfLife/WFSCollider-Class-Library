UScoreEditorGui_TransportBar {
    var <scoreView;
    var <>views, <>scoreController, <scoreViewController;

    *new{ |parent, bounds, scoreView|
        ^super.newCopyArgs(scoreView).init(parent, bounds)
    }

    init{ |parent, bounds|
        this.makeGui(parent, bounds);
        scoreViewController = SimpleController( scoreView );
        scoreViewController.put(\activeScoreChanged, {
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

		scoreController.put(\playState,{ |a,b,newState,oldState|
		    //[newState,oldState].postln;
		    if( newState == \playing )  {
		        views[\play].value = 1;
		        { views[\prepare].stop }.defer
		    };
		    if(newState == \stopped ) {
		        { views[\prepare].stop; }.defer;
                views[\pause].value = 0;
                views[\play].value = 0;
		    };
		    if( newState == \preparing ) {
		        { views[\prepare].start }.defer
		    };
		    //resuming
		    if( (newState == \playing) && (oldState == \paused) ) {
		        views[\pause].value = 0;
		    };
		    if( newState == \prepared ) {

                { views[\prepare].stop }.defer;
                views[\play].value = 2;

		    };

		});

		scoreController.put(\paused, {
            views[\pause].value = 1;
		});

		scoreController.put(\start, {
            views[\play].value = 1;
		});

		scoreController.put(\pos, { |who,what,pos|
            views[\counter].value = pos;
		});

		views[\play].value = this.score.isPlaying.binaryValue;
		views[\pause].value = this.score.isPaused.binaryValue;
		if(this.score.isPreparing) {
		    { views[\prepare].start }.defer
		} {
		    { views[\prepare].stop }.defer
		}


    }

    remove {
        [scoreController,scoreViewController].do(_.remove)
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
			    [ \stop, Color.black, Color(0.40298507462687, 0.73134328358209, 0.44776119402985) ],
			    [ \play, Color.blue, Color.red ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			//.changeStateWhenPressed_(false)
			.action_({  |v,c,d,e|

			    var startedPlaying;
			    switch( v.value ) {1} {
                    startedPlaying = this.score.prepareAndStart( UServerCenter.servers, this.score.pos);
			        if( startedPlaying.not ){ v.value = 0 };
			    }{2} {
                    this.score.stop;
                    views[\pause].value = 0;
                    { views[\prepare].stop }.defer;
                    v.value = 0;
			    } {
			        views[\pause].value = 0;
			        startedPlaying = this.score.start( UServerCenter.servers, this.score.pos);
			        if( startedPlaying.not ){ v.value = 0 }{v.value = 1};
			    }


			});
			
		views[\pause] = SmoothButton( view, 50@size  )
			.states_( [
			    [ \pause, Color.black, Color.clear ],
			    [ \pause, Color.red,Color(0.40298507462687, 0.73134328358209, 0.44776119402985) ],
			    [ \pause, Color.blue,Color.red ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.background_(Color.grey(0.8))
			.action_({ |v|
			    switch( v.value)
			    {1}{
			        if(this.score.isPlaying) {
			        this.score.pause;
			       } {
			        v.value = 2;
			         this.score.prepare;
			       }
			    }{2} {
			        v.value = 0;
			        this.score.resume(UServerCenter.servers);
			    }{
			        this.score.stop;
			        views[\play].value = 0;

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
            .action_({ |v|
                if(this.score.isStopped) {
                    this.score.pos = v.value
                }
            });

    }

}