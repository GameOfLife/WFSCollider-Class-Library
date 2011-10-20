USessionGUI : UAbstractWindow {

	var <session;
    var <sessionView, bounds;
    var <sessionController, objectControllers;

    *new { |session, bounds|
        ^super.new.init( session)
			.addToAll
			.makeGui(bounds)
	}

	init { |inSession|
	    session = inSession;
	    sessionController = SimpleController(session);
	    sessionController.put(\objectsChanged,{
	        { this.makeSessionView }.defer(0.05);
	    });
	    sessionController.put(\name,{
            window.name = this.windowTitle
        })
	}

    windowTitle {
        ^("Session Editor : "++this.session.name)
    }

    remove {
        (objectControllers++[sessionController]).do(_.remove)
    }

	makeGui { |bounds|
        var topBarView;
		var font = Font( Font.defaultSansFace, 11 );

        var topBarHeigth = 40;
        var size = 16;
        var margin = 4;
        var gap = 2;
        bounds = bounds ? Rect(100,100,600,400);
        this.newWindow(bounds, "USession - "++session.name,{ this.remove }, margin:0, gap:0);
        topBarView =  CompositeView(view, Rect(0,0,bounds.width,topBarHeigth));
        topBarView.addFlowLayout;

        SmoothButton( topBarView, 40@size  )
			.states_( [
			    [ \play, Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    session.startAll
			});

		SmoothButton( topBarView, 40@size  )
			.states_( [
			    [ \stop, Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    session.stopAll
			});

        this.makeSessionView;

    }

    makeSessionView {
        var topBarHeigth = 40;
        var margin = 4;
        var gap = 2;
		var font = Font( Font.defaultSansFace, 11 );
        var bounds = view.bounds.moveTo(0,0);
        if(sessionView.notNil) {
            sessionView.remove;
        };
        sessionView = CompositeView(view, Rect(0,topBarHeigth,bounds.width,bounds.height - topBarHeigth));
        sessionView.addFlowLayout;
        session.objects.do { |object|
            var releaseTask, but, ctl;

            StaticText(sessionView,100@16)
                .string_(object.name);

            SmoothButton(sessionView,25@16)
                .states_([[\up,Color.black,Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    object.gui
			    });

			SmoothButton(sessionView,25@16)
                .states_([["-",Color.black,Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    if(object.class != UScore){
                        session.remove(object);
                        { this.makeSessionView; window.refresh; }.defer(0.1)
                    } {
                        if( (object.events.size != 0) && (object.isDirty) ) {
                            SCAlert( "Do you want to save your score? (" ++ object.name ++ ")" ,
                            [ [ "Don't save" ], [ "Cancel" ], [ "Save" ],[ "Save as"] ],
                            [ 	{session.remove(object)},
                                nil,
                                { object.save({session.remove(object)}) },
                                { object.saveAs(nil,{session.remove(object)}) }
                            ] );

                        }
                    }
			    });

			if(object.class == UScore){
			    UTransportView(object, sessionView, 16)
			};

		   if( object.isUChainLike ) {
			    sessionView.decorator.shift(18,0);
			    but = SmoothButton( sessionView, 40@16  )
                    .label_( ['power','power'] )
                    .hiliteColor_( Color.green.alpha_(0.5) )
                    .canFocus_(false)
                    .font_( font )
                    .border_(1).background_(Color.grey(0.8))
                    .action_( [ {
                        object.prepareAndStart;
                    }, {
                        object.release
                    } ]
                    );

                if( object.groups.size > 0 ) {
                    but.value = 1;
                };
           };
           if( object.class == UChain) {
                ctl = SimpleController(object);
                objectControllers.add(ctl);
                ctl
                .put( \start, { but.value = 1 } )
                .put( \end, {
                    if( object.units.every({ |unit| unit.synths.size == 0 }) ) {
                        but.value = 0;
                    };
                } )

			};
			if( object.class == UScoreList) {
			    UTransportView(object.metaScore, sessionView, 16);
			};
			sessionView.decorator.nextLine;


        };
        window.refresh;
    }

}
