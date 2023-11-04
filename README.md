# forex-mtl
Forex is a simple application that acts as a local proxy for getting exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies, so they don't have to care about the specifics of third-party providers.

## Getting Started

Before running the Forex app, make sure to set the following environment variables:

- `ONE_FRAME_HOST`: The host or URL of the OneFrame service.
- `ONE_FRAME_PORT`: The port number for the OneFrame service.
- `ONE_FRAME_TOKEN`: Your authentication token for the OneFrame service.

These environment variables are required to establish a connection with OneFrame, a third-party provider of exchange rate data. They allow the Forex app to retrieve up-to-date exchange rates.
