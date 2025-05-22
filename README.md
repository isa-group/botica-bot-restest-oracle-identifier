# Oracle Identifier Bot for RESTest

This bot collects test execution data
from [RESTest reporter bots](https://github.com/isa-group/botica-bot-restest-reporter/) until it
reaches a minimum threshold of test cases, then analyzes the collected data to identify invariants
that consistently hold across API responses, using the AGORA (Automated Generation of test Oracles
for REST APIs) framework.

## Setup

- `ORACLE_BOT_MIN_TEST_CASES`: Minimum number of test cases required before triggering invariant
  detection (default: 50)

## Integration with other bots

- Identified invariants will be sent to the user via Telegram if using
  the [Telegram frontend bot](https://github.com/isa-group/botica-bot-telegram-frontend/) within the
  infrastructure.

## Credits

This bot makes use of AGORA (Automated Generation of test Oracles for REST APIs), the first
approach for the automated generation of test oracles for REST APIs in a black-box context.

Alonso, Juan C. and Segura, Sergio and Ruiz-Cortés, Antonio. AGORA: Automated
Generation of test Oracles for REST APIs.
