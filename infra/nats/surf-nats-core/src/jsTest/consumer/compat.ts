// The hand-written facade must describe the Kotlin/JS exports. Its generic operations, instantiated with the
// descriptor base types, must fit the Kotlin signatures: Kotlin accepts every argument the facade accepts, and
// every result the facade promises is one the Kotlin signature allows.
import type { DescMessage, DescMethod } from "@bufbuild/protobuf";
import type * as Facade from "@slne/surf-nats";
import type * as Kotlin from "./node_modules/@slne/surf-nats/@slne/surf-nats.mjs";

type Fn = (...args: never[]) => unknown;

type Fits<F extends Fn, K extends Fn> =
    Parameters<F> extends Parameters<K>
        ? Awaited<ReturnType<F>> extends Awaited<ReturnType<K>> ? true : false
        : false;

declare const facade: Facade.SurfNatsOperations;

type OperationsFit<K extends Kotlin.SurfNatsClient> = [
    Fits<typeof facade.publish<DescMessage>, K["publish"]>,
    Fits<typeof facade.listen<DescMessage>, K["listen"]>,
    Fits<typeof facade.call<DescMethod>, K["call"]>,
    Fits<typeof facade.callAnswer<DescMethod>, K["callAnswer"]>,
];

export const clientOperations: OperationsFit<Kotlin.SurfNatsClient> = [true, true, true, true];

declare const kotlinClient: Kotlin.SurfNatsClient;
declare const kotlinRegistration: Kotlin.NatsRegistration;
declare const kotlinConnect: typeof Kotlin.connect;

export const client: Pick<Facade.SurfNatsClient, "name" | "origin" | "isClosed" | "close"> = kotlinClient;
export const registration: Facade.NatsRegistration = kotlinRegistration;
export const connectFn: (options: Facade.ConnectOptions) => Promise<Kotlin.SurfNatsClient> = kotlinConnect;
