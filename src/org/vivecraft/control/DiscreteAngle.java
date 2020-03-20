package org.vivecraft.control;

/**
 * StellaArtois 12/10/2014.
 */
public class DiscreteAngle
{
    public double _delta   = 0d;
    public double _actual  = 0d;
    public double _currentReported = 0d;

    public double _moveStart      = 0d;
    public double _moveEnd        = 0d;
    public double _moveStartTime  = 0d;
    public double _moveEndTime    = 0d;
    public double _percent        = -1d;

    public double  _trigger            = 0d;
    public double  _transitionTimeSecs = 0d;
    public boolean _linear = true;

    public DiscreteAngleState _state = DiscreteAngleState.TRACKING;

    public void configure(double triggerAngle, double transitionTimeSecs, boolean linear)
    {
        if (_trigger != triggerAngle)
        {
            _trigger = triggerAngle;

            // Ensure we snap to the nearest trigger angle initially...
            _state           = DiscreteAngleState.TRACKING;
            _currentReported = Math.round(_currentReported / _trigger) * _trigger;
            _delta           = 0d;
        }
        _transitionTimeSecs = transitionTimeSecs;
        _linear             = linear;
    }

    public void resetAngle()
    {
        _delta   = 0d;
        _actual  = 0d;
        _currentReported = 0d;

        _moveStart      = 0d;
        _moveEnd        = 0d;
        _moveStartTime  = 0d;
        _moveEndTime    = 0d;
        _percent        = -1d;

        _state = DiscreteAngleState.TRACKING;
    }

    public void update(float deltaAngleDeg, double currentTimeSecs)
    {
        _actual += deltaAngleDeg;

        // Only record delta movement during tracking state. We need to limit the maximum
        // change while in state 'changing', so we never 'buffer' too much movement.
        if (_state != DiscreteAngleState.CHANGING)
        {
            _delta += deltaAngleDeg;
        }

        if (_trigger == 0d)
        {
            _state          = DiscreteAngleState.TRACKING;
            _moveStart     = 0d;
            _moveEnd       = 0d;
            _moveStartTime = 0d;
            _moveEndTime   = 0d;
            _currentReported = _actual;
            _delta         = 0d;
        }
        else if (_state == DiscreteAngleState.TRACKING && _delta >= _trigger)
        {
            _state         = DiscreteAngleState.CHANGING;
            _moveStart     = _currentReported;
            _moveEnd       = _currentReported + _trigger;
            _moveStartTime = currentTimeSecs;
            _moveEndTime   = currentTimeSecs + _transitionTimeSecs;
            _delta        -= _trigger;
        }
        else if (_state == DiscreteAngleState.TRACKING && _delta <= -_trigger)
        {
            _state         = DiscreteAngleState.CHANGING;
            _moveStart     = _currentReported;
            _moveEnd       = _currentReported - _trigger;
            _moveStartTime = currentTimeSecs;
            _moveEndTime   = currentTimeSecs + _transitionTimeSecs;
            _delta        += _trigger;
        }
    }

    public void triggerChange(boolean isPositive) {
        if (isPositive) {
            _delta += _trigger;
        }
        else {
            _delta -= _trigger;
        }
    }

    public double getCurrent(double currentTimeSecs)
    {
        double angle = _currentReported;

        if (_state == DiscreteAngleState.CHANGING)
        {
            if (_moveEndTime < currentTimeSecs)
            {
                _currentReported = angle = _moveEnd;
                _state         = DiscreteAngleState.TRACKING;
                _moveStart     = 0d;
                _moveEnd       = 0d;
                _moveStartTime = 0d;
                _moveEndTime   = 0d;
                _percent       = -1d;
            }
            else
            {
                // Calculate here to prevent changes in configuration screwing things up
                // during the move
                double transitionTime = _moveEndTime - _moveStartTime;
                double moveDistance   = _moveEnd - _moveStart;
                double elapsedTime    = currentTimeSecs - _moveStartTime;
                _percent              = (100d / transitionTime) * elapsedTime;
                //System.out.println("Percent: " + _percent);

                if (_linear)
                {
                    // Linear movement
                    angle = _currentReported + ((moveDistance / transitionTime) *
                            elapsedTime);
                }
                else
                {
                    // Sinusoidal (Cosusoidal?!) to give slight acceleration / de-acceleration to movement
                    angle = _currentReported + ((((Math.cos((Math.PI + ((Math.PI /
                            transitionTime) * elapsedTime))) + 1d)) / 2d) * moveDistance);
                }
            }
        }

        return angle;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb
            .append("Actual=").append(_actual).append("/n")
            .append("Delta=").append(_delta).append("/n")
            .append("Reported=").append(_currentReported).append("/n")
            .append("MoveStart=").append(_moveStart).append("/n")
            .append("MoveEnd=").append(_moveEnd).append("/n")
            .append("MoveStartTime=").append(_moveStartTime).append("/n")
            .append("MoveEndTime=").append(_moveEndTime).append("/n")
            .append("Trigger=").append(_trigger).append("/n")
            .append("TransitionTimeSecs=").append(_transitionTimeSecs).append("/n")
            .append("State=").append(_state.toString()).append("/n");

        return sb.toString();
    }

    public enum DiscreteAngleState
    {
        TRACKING(0),
        CHANGING(1);

        private int value;

        DiscreteAngleState(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public String toString()
        {
            switch(this)
            {
            case TRACKING:
                return "TRACKING";
            }

            return "CHANGING";
        }
    }
}
