// Copy this file into your app target and register it once before a run:
//
//     let backend = FoundationModelsBackend(bridge: ProctorFoundationModelsBridge())
//
// The import below covers the two frameworks this repository builds, ProctorIos and the sample's Shared. Add the
// name of your own Kotlin framework that exports `proctor-ios`. Requires iOS 26 or newer.

import Foundation
import FoundationModels
#if canImport(ProctorIos)
import ProctorIos
#elseif canImport(Shared)
import Shared
#endif

@available(iOS 26.0, macOS 26.0, *)
public final class ProctorFoundationModelsBridge: NSObject, FoundationModelsBridge {
    public override init() {
        super.init()
    }

    public func availability() -> BridgeAvailability {
        switch SystemLanguageModel.default.availability {
        case .available:
            return .available
        case .unavailable(.deviceNotEligible):
            return .deviceNotEligible
        case .unavailable(.appleIntelligenceNotEnabled):
            return .appleIntelligenceNotEnabled
        case .unavailable(.modelNotReady):
            return .modelNotReady
        case .unavailable:
            // A reason added by a newer OS: report it as not ready rather than as a prompt failure.
            return .modelNotReady
        }
    }

    public func generate(request: BridgeRequest, completion: BridgeCompletion) {
        Task {
            do {
                // A new session per request: no transcript carries over between cases or repeats.
                let session = LanguageModelSession()
                let response = try await session.respond(to: request.prompt, options: Self.options(for: request))
                completion.succeed(text: response.content)
            } catch let error as LanguageModelSession.GenerationError {
                completion.fail(kind: Self.kind(of: error), message: error.localizedDescription)
            } catch {
                completion.fail(kind: .other, message: error.localizedDescription)
            }
        }
    }

    private static func options(for request: BridgeRequest) -> GenerationOptions {
        let mode: GenerationOptions.SamplingMode?
        if let topK = request.topK?.intValue {
            mode = .random(top: topK, seed: request.seed.map { UInt64(truncating: $0) })
        } else if request.temperature?.doubleValue == 0 {
            mode = .greedy
        } else {
            mode = nil
        }
        return GenerationOptions(
            samplingMode: mode,
            temperature: request.temperature?.doubleValue,
            maximumResponseTokens: request.maxOutputTokens?.intValue
        )
    }

    private static func kind(of error: LanguageModelSession.GenerationError) -> BridgeErrorKind {
        switch error {
        case .rateLimited: return .rateLimited
        case .concurrentRequests: return .concurrentRequests
        case .assetsUnavailable: return .assetsUnavailable
        case .exceededContextWindowSize: return .exceededContextWindowSize
        case .guardrailViolation: return .guardrailViolation
        case .refusal: return .refusal
        case .unsupportedLanguageOrLocale: return .unsupportedLanguageOrLocale
        default: return .other
        }
    }
}
