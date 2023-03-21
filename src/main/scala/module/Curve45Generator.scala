package metarules.module

import metarules.meta._
import Network._, Flags._, Flag._, RotFlip._, Implicits._

/* Flags of sharp curves:
 *       +---------+---------+
 *       |         |         |
 *       |         |       +1|
 *       |         |   -3    |
 *       +---------+---------+
 *       |         |   +3    |
 *       |-2    +11|-11      |
 *       |         |         |
 *       +---------+---------+
 * Flags of R1 curves (single-tile networks):
 *       +---------+---------+---------+
 *       |         |         |   +3    |
 *       |         |       +1|-1       |
 *       |         |  -113   |         |
 *       +---------+---------+---------+
 *       |         |  +113   |         |
 *       |-2   +121|-121     |         |
 *       |         |         |         |
 *       +---------+---------+---------+
 * Flags of mini curves (multi-tile networks):
 *       +---------+---------+---------+
 *       |         |         |   +3    |
 *       |         |       +1|-1       |
 *       |         |  -113   |         |
 *       +---------+---------+---------+
 *       |         |  +113   |         |
 *       |-2    +11|-11      |         |
 *       |         |         |         |
 *       +---------+---------+---------+
 * Flags of extended curves:
 *       +---------+---------+---------+---------+
 *       |         |         |         |   +3    |
 *       |         |         |       +1|-1       |
 *       |         |         |  -113   |         |
 *       +---------+---------+---------+---------+
 *       |         |         |  +113   |         |
 *       |-2   +111|-111  +11|-11      |         |
 *       |         |         |         |         |
 *       +---------+---------+---------+---------+
 */

trait Stability { _: RuleGenerator =>
  def stabilize(rule: Rule[SymTile]): Seq[Rule[SymTile]] = {
    if (rule(0) == rule(2) || rule(1) == rule(3)) {
      Seq(rule)
    } else { // TODO handle corner cases
      Seq(rule, Rule(rule(0), rule(3), rule(2), rule(3)), Rule(rule(2), rule(1), rule(2), rule(3)))
    }
  }
}

trait Curve45Generator extends Stability { _: RuleGenerator =>

  private def hasSharedDiagCurve(n: Network): Boolean = n.typ == AvenueLike

  private def hasSharpCurveBase(n: Network, inside: Boolean): Boolean = {
    n.base.exists(b => b == Dirtroad || b == Road || b == Onewayroad)
  }

  private def hasR1CurveBase(n: Network): Boolean = {
    n.base.exists(b => b == Dirtroad || b == Road || b == Onewayroad)
  }

  private def hasSharpCurve(n: Network, inside: Boolean): Boolean = {
    n >= L1Rhw2 && n <= L4Rhw6s || n >= Tla3 && n <= Nrd4 || n == Tla5 || n == Rd6 || n == Owr5 ||
    inside && (n == Ave6 || n == Ave8)
  }

  private def hasMiniCurve(n: Network, inside: Boolean): Boolean = {
    inside && (n >= Rhw8s && n <= L2Rhw10c && (n < Rhw6cm || n > L2Rhw6cm)) ||
    !inside && (n >= Rhw8sm && n <= L2Rhw8sm) ||
    n == Ave6m || n == Tla7m
  }

  private def hasExtendedCurve(n: Network, inside: Boolean): Boolean = {
    (n.isRhw && n > L4Rhw6s && n <= L2Rhw10c) && !hasMiniCurve(n, inside) ||
    !inside && (n == Ave6 || n == Ave8)
  }

  private def hasR1Curve(n: Network): Boolean = {
    (n.isRhw && (n >= Dirtroad && n <= L4Rhw6s && n != L1Rhw3 && n != L2Rhw3))  // elevated R1 Rhw3 models are currently missing
    // TODO add NWM
  }

  def createCurve45Rules(main: Network): Unit = {
    // all curves are written in form of outside curve; orient can be used
    // to reverse the flags in order to create the corresponding inside
    // curves
    def curveCode(inside: Boolean): Unit = {
      val orient: IntFlags => IntFlags = if (!inside) identity else reverseIntFlags
      assert(main.base.isDefined)
      val base = main.base.get
      if (hasSharedDiagCurve(main)) {
        // orth to diag
        Rules += main~WE~EW                         | (base ~> main)~(-2,0,+11,0)~(+2,0,-11,0)
        Rules += main~(-2,0,+11,0)~(+2,0,-11,0)     | (base ~> main)~(-11,+3,0,0)~(+11,-3,+1,-3)
        Rules += main~(0,-11,+3,0)                  | (base ~> main)~(-3,+11,-3,+1)
        Rules += main~(+11,-3,+1,-3)~(-3,+11,-3,+1) | (base ~> main)~WN~SW
        // diag to orth
        Rules += (base ~> main)~WE~EW                         | main~(-2,0,+11,0)~(+2,0,-11,0)
        Rules += (base ~> main)~(-2,0,+11,0)~(+2,0,-11,0)     | main~(-11,+3,0,0)~(+11,-3,+1,-3)
        Rules += (base ~> main)~(0,-11,+3,0)                  | main~(-3,+11,-3,+1)
        Rules += (base ~> main)~(+11,-3,+1,-3)~(-3,+11,-3,+1) | main~WN~SW
      }
      if (hasSharpCurveBase(main, inside)) {
        if (hasSharpCurve(main, inside)) {
          // orth to diag
          Rules += main~orient(WE)         | (base ~> main)~orient(-2,0,+11,0)
          Rules += main~orient(-2,0,+11,0) | (base ~> main)~orient(-11,+3,0,0)
          Rules += main~orient(0,-11,+3,0) | (base ~> main)~orient(WS)
          // diag to orth
          Rules += (base ~> main)~orient(WE)         | main~orient(-2,0,+11,0)
          Rules += (base ~> main)~orient(-2,0,+11,0) | main~orient(-11,+3,0,0)
          Rules += (base ~> main)~orient(0,-11,+3,0) | main~orient(WS)
        } else if (hasMiniCurve(main, inside)) {
          // orth to diag
          Rules += main~orient(WE)          | (base ~> main)~orient(-2,0,+11,0)
          Rules += main~orient(-2,0,+11,0)  | base~orient(-11,+3,0,0)    | % | main~orient(-11,+113,0,0)
          Rules += main~orient(0,-11,+113,0) | base~orient(WS)            | % | main~orient(-113,0,0,+1)
          Rules += main~orient(0,0,+1,-113)  | (base ~> main)~orient(WN)
          // diag to orth
          Rules += (base ~> main)~orient(WE)         | main~orient(-2,0,+11,0)
          Rules += (base ~> main)~orient(-2,0,+11,0) | main~orient(-11,+113,0,0)
          Rules ++= stabilize (
            base~orient(0,-11,+3,0) | main~orient(WS) | main~orient(0,-11,+113,0) | main~orient(-113,0,0,+1)
          )
        } else if (hasExtendedCurve(main, inside)) {
          // orth to diag
          Rules ++= stabilize (
            main~orient(WE) | base~orient(-2,0,+11,0) | main~orient(-2,0,+111,0) | main~orient(-111,0,+11,0)
          )
          Rules += main~orient(-111,0,+11,0) | base~orient(-11,+3,0,0) | % | main~orient(-11,+113,0,0)
          Rules += main~orient(0,-11,+113,0)  | base~orient(WS)         | % | main~orient(-113,0,0,+1)
          Rules += main~orient(0,0,+1,-113)   | (base ~> main)~orient(WN)
          // diag to orth
          Rules += (base ~> main)~orient(WE) | main~orient(-2,0,+111,0)
          Rules += base~orient(WE) | main~orient(-111,0,+11,0) | main~orient(-2,0,+111,0) | %
          Rules += base~orient(-2,0,+11,0) | main~orient(-11,+113,0,0) | main~orient(-111,0,+11,0) | %
          Rules ++= stabilize (
            base~orient(0,-11,+3,0) | main~orient(WS) | main~orient(0,-11,+113,0) | main~orient(-113,0,0,+1)
          )
        }
      }
      if (hasR1CurveBase(main)) {
        if (hasR1Curve(main)) {
          // R1 curve: orth to diag
          Rules += main~orient(WE)            | (base ~> main)~orient(-2,0,+121,0)
          Rules += main~orient(-2,0,+121,0)   | (base ~> main)~orient(-121,+113,0,0)
          Rules += main~orient(0,-121,+113,0) | (base ~> main)~orient(-113,0,0,+1)
          Rules += main~orient(0,-121,+113,0) | (base ~> main)~orient(-113,0,0,+111)  // R1 curve: 90 degree
          Rules += main~orient(0,0,+1,-113)   | (base ~> main)~orient(WN)
          // R1 curve: diag to orth
          Rules += (base ~> main)~orient(WE)            | main~orient(-2,0,+121,0)
          Rules += (base ~> main)~orient(-2,0,+121,0)   | main~orient(-121,+113,0,0)
          Rules += (base ~> main)~orient(0,-121,+113,0) | main~orient(-113,0,0,+1)
          Rules += (base ~> main)~orient(0,-121,+113,0) | main~orient(-113,0,0,+111)  // R1 curve: 90 degree
          Rules += (base ~> main)~orient(0,0,+1,-113)   | main~orient(WN)
          // R1 curve: S-type curves
          Rules += main~orient(0,0,+1,-113)   | (base ~> main)~orient(-1,+113,0,0)
          Rules += main~orient(-121,0,+2,0)   | (base ~> main)~orient(-2,0,+121,0)  // trans
          Rules += main~orient(-123,0,+2,0)   | (base ~> main)~orient(-2,0,+121,0)  // cis
        }
      }
    }
    curveCode(inside = false)
    if (!main.isSymm && !hasSharedDiagCurve(main)) {
      curveCode(inside = true)
    }
    createRules()
  }
}
