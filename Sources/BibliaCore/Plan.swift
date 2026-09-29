/// Las lecturas de un día del plan
public struct LecturaDelDia: Identifiable, Hashable, Sendable {
    public let dia: Int
    public let referencias: [String]

    public var id: Int { dia }
}

public let tituloDelPlan = "Plan de Lectura Bíblica - Septiembre"

public let planSeptiembre: [LecturaDelDia] = [
    LecturaDelDia(dia: 1, referencias: ["Juan 9:1-23", "2 Crónicas 6", "Malaquías 2:17-3:18"]),
    LecturaDelDia(dia: 2, referencias: ["Juan 9:24-41", "2 Crónicas 7", "Malaquías 4"]),
    LecturaDelDia(dia: 3, referencias: ["Juan 10:1-21", "2 Crónicas 8", "Salmos 73"]),
    LecturaDelDia(dia: 4, referencias: ["Juan 10:22-42", "2 Crónicas 9", "Salmos 74"]),
    LecturaDelDia(dia: 5, referencias: ["Juan 11:1-27", "2 Crónicas 10-11", "Salmos 75"]),
    LecturaDelDia(dia: 6, referencias: ["Juan 11:28-57", "2 Crónicas 12-13", "Salmos 76"]),
    LecturaDelDia(dia: 7, referencias: ["Juan 12:1-26", "2 Crónicas 14-15", "Salmos 77"]),
    LecturaDelDia(dia: 8, referencias: ["Juan 12:27-50", "2 Crónicas 16-17", "Salmos 78:1-20"]),
    LecturaDelDia(dia: 9, referencias: ["Juan 13:1-20", "2 Crónicas 18", "Salmos 78:21-37"]),
    LecturaDelDia(dia: 10, referencias: ["Juan 13:21-38", "2 Crónicas 19", "Salmos 78:38-55"]),
    LecturaDelDia(dia: 11, referencias: ["Juan 14:1-14", "2 Crónicas 20:1-21:1", "Salmos 78:56-72"]),
    LecturaDelDia(dia: 12, referencias: ["Juan 14:15-31", "2 Crónicas 21:2-22:12", "Salmos 79"]),
    LecturaDelDia(dia: 13, referencias: ["Juan 15:1-16:4", "2 Crónicas 23", "Salmos 80"]),
    LecturaDelDia(dia: 14, referencias: ["Juan 16:4-33", "2 Crónicas 24", "Salmos 81"]),
    LecturaDelDia(dia: 15, referencias: ["Juan 17", "2 Crónicas 25", "Salmos 82"]),
    LecturaDelDia(dia: 16, referencias: ["Juan 18:1-18", "2 Crónicas 26", "Salmos 83"]),
    LecturaDelDia(dia: 17, referencias: ["Juan 18:19-27", "2 Crónicas 27-28", "Salmos 84"]),
    LecturaDelDia(dia: 18, referencias: ["Juan 18:28-19:16", "2 Crónicas 29", "Salmos 85"]),
    LecturaDelDia(dia: 19, referencias: ["Juan 19:17-42", "2 Crónicas 30", "Salmos 86"]),
    LecturaDelDia(dia: 20, referencias: ["Juan 20:1-18", "2 Crónicas 31", "Salmos 87"]),
    LecturaDelDia(dia: 21, referencias: ["Juan 20:19-31", "2 Crónicas 32", "Salmos 88"]),
    LecturaDelDia(dia: 22, referencias: ["Juan 21", "2 Crónicas 33", "Salmos 89:1-18"]),
    LecturaDelDia(dia: 23, referencias: ["1 Juan 1", "2 Crónicas 34", "Salmos 89:19-37"]),
    LecturaDelDia(dia: 24, referencias: ["1 Juan 2", "2 Crónicas 35", "Salmos 89:38-52"]),
    LecturaDelDia(dia: 25, referencias: ["1 Juan 3", "2 Crónicas 36", "Salmos 90"]),
    LecturaDelDia(dia: 26, referencias: ["1 Juan 4", "Esdras 1-2", "Salmos 91"]),
    LecturaDelDia(dia: 27, referencias: ["1 Juan 5", "Esdras 3-4", "Salmos 92"]),
    LecturaDelDia(dia: 28, referencias: ["2 Juan 1", "Esdras 5-6", "Salmos 93"]),
    LecturaDelDia(dia: 29, referencias: ["3 Juan 1", "Esdras 7-8", "Salmos 94"]),
    LecturaDelDia(dia: 30, referencias: ["Judas 1", "Esdras 9-10", "Salmos 95"])
]
