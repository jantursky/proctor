import SwiftUI
import UIKit

/// The colours of design/tokens.css, with the derived dark mode.
enum Palette {
    static let canvas = dynamic(light: 0xFFFFFF, dark: 0x0B0F19)
    static let band = dynamic(light: 0xF3F4F6, dark: 0x101828)
    static let hairline = dynamic(light: 0xE5E7EB, dark: 0x2C3344)
    static let ink = dynamic(light: 0x000000, dark: 0xF9FAFB)
    static let onInk = dynamic(light: 0xFFFFFF, dark: 0x0B0F19)
    static let body = dynamic(light: 0x4A5565, dark: 0xD1D5DC)
    static let caption = dynamic(light: 0x6A7282, dark: 0x99A1AF)
    static let pass = dynamic(light: 0x16794C, dark: 0x4ADE80)
    static let fail = dynamic(light: 0xB42318, dark: 0xF87171)
    static let notGraded = dynamic(light: 0xA15C07, dark: 0xFBBF24)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in
            let value = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(
                red: CGFloat((value >> 16) & 0xFF) / 255,
                green: CGFloat((value >> 8) & 0xFF) / 255,
                blue: CGFloat(value & 0xFF) / 255,
                alpha: 1
            )
        })
    }
}

struct CardStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.hairline, lineWidth: 1))
    }
}

extension View {
    func card() -> some View { modifier(CardStyle()) }
}

struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .medium))
            .tracking(0.4)
            .foregroundStyle(Palette.caption)
    }
}
