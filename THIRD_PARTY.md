# Third-party licenses and attribution

Goshawk retains the original root `LICENSE`, upstream source notices, Git
history, and the unmodified upstream README in `docs/upstream/SPARROW-README.md`.
There was no standalone NOTICE file in the pinned Sparrow checkout; Goshawk's
NOTICE adds attribution without replacing an upstream notice.

| Component | Retained license | Treatment |
| --- | --- | --- |
| Sparrow Wallet | `LICENSE` (Apache-2.0) | Original license and source history retained |
| Drongo | `drongo/LICENSE` (Apache-2.0) | Pinned, unmodified submodule |
| Lark | `lark/LICENSE` (Apache-2.0) | Goshawk-maintained fork, pinned commit; upstream notices retained |
| Space Grotesk | `scripts/brand/OFL.txt` (SIL OFL 1.1) | Used for outlined brand lettering |
| Other runtime dependencies | Component-specific | Existing notices retained; embedded notices extracted during packaging |

`collectThirdPartyNotices` copies LICENSE, LICENCE, NOTICE, COPYING, COPYRIGHT
and OFL files from resolved runtime JARs without rewriting their contents. It
also records the exact component coordinates and any artifacts with no embedded
notice in `COMPONENTS.tsv`. This is preservation, not a claim that every runtime
dependency is Apache-2.0 or that a complete distribution audit has been done.
In particular, separately bundled executables and native libraries may carry
additional source-availability or redistribution obligations. Review the
inventory and those components before a public binary release.

The application JAR includes the project license, NOTICE, modification summary,
and Drongo/Lark/font licenses under `/legal`. Native application images include
these plus the extracted runtime notices under `runtime/legal/goshawk` (inside
the bundled runtime; the outer path varies by platform). Gradle distributions
include their own `legal/goshawk` directory.

The About window displays the project license and attribution locally, without
requiring a network connection. The original upstream donation link is retained
and explicitly labelled “Support Sparrow”. Goshawk has no Sparrow signing key
or entitlement to use upstream release credentials.

The Ledger behavior/reference matrix is pinned to ElectrumSVP
`fff115ea3233ad7045c7a67a9479a9d1e1310c9e`. Its Open BSV-licensed Python
source is not incorporated; the Java changes extend Lark and retain Apache-2.0.
See `lark/MODIFICATIONS-GOSHAWK.md` and `lark/docs/bsv-ledger.md`.

Goshawk also maintains a [Drongo fork](https://github.com/linzhm/drongo) for BSV
transaction rules. Its original Apache-2.0 license remains unchanged. BitcoinX
vectors use public test keys only; BitcoinX is not shipped as a runtime dependency.
