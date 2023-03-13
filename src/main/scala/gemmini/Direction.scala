package gemmini

object Direction extends Enumeration {
  type Direction = Value
  val Forward, Backward, Bidirectional, Disabled = Value
  // Forward is defined as Left to Right and and Top to Bottom. In the case of Anti-diagonal the Left to Right takes precedence.
}

case class DirectionConfig(
  val Vertical : Direction.Value = Direction.Disabled, 
  val Horizontal : Direction.Value = Direction.Disabled, 
  val Diagonal : Direction.Value = Direction.Disabled, 
  val AntiDiagonal : Direction.Value = Direction.Disabled)

object CommonDirectionConfigs {
  // Dataflow direction of the unmodified Gemmini
  val OrthogonalForwad = DirectionConfig(Vertical = Direction.Forward, Horizontal = Direction.Forward)
  val OrthogonalAndDiagonalForward = DirectionConfig(Vertical = Direction.Forward, Horizontal = Direction.Forward, Diagonal = Direction.Forward)
  val AllForward = DirectionConfig(Vertical = Direction.Forward, Horizontal = Direction.Forward, Diagonal = Direction.Forward, AntiDiagonal = Direction.Forward)
  val AllBidirectional = DirectionConfig(Vertical = Direction.Bidirectional, Horizontal = Direction.Bidirectional, Diagonal = Direction.Bidirectional, AntiDiagonal = Direction.Bidirectional)
  val OrthogonalBidirectional = DirectionConfig(Vertical = Direction.Bidirectional, Horizontal = Direction.Bidirectional)
}